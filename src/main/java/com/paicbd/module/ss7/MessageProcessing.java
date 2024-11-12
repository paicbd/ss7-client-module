package com.paicbd.module.ss7;

import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.CustomNumberingPlanIndicator;
import com.paicbd.module.utils.CustomTypeOfNumber;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.RequestDelivery;
import com.paicbd.smsc.utils.UtilsEnum;
import com.paicbd.smsc.utils.Watcher;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.util.DeliveryReceiptState;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.MAPMessageType;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.restcomm.protocols.ss7.sccp.NetworkIdState;
import org.restcomm.protocols.ss7.tcap.asn.ReturnResultLastImpl;
import org.restcomm.protocols.ss7.tcap.asn.comp.ReturnResultLast;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.paicbd.module.utils.Constants.ABSENT_SUBSCRIBER_HASH_NAME;


@Slf4j
public class MessageProcessing {
    private final MapLayer mapLayer;
    private final JedisCluster jedisCluster;
    private final Gateway currentGateway;
    private final String redisMessageRetryQueue;
    private final String redisMessageList;
    private final CdrProcessor cdrProcessor;
    private final ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;
    private final MessageFactory messageFactory;

    private static final ConcurrentHashMap<Long, MessageEvent> messageSriConcurrentHashMap = new ConcurrentHashMap<>(128, 0.75f, 100);
    private static final ConcurrentHashMap<Long, MessageEvent> messageMtConcurrentHashMap = new ConcurrentHashMap<>(128, 0.75f, 100);
    private final ThreadFactory factory = Thread.ofVirtual().name("MessageProcessing - ").factory();
    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(factory);

    AtomicInteger sriRequestPerSecond = new AtomicInteger(0);
    AtomicInteger sriResponsePerSecond = new AtomicInteger(0);
    AtomicInteger mtRequestPerSecond = new AtomicInteger(0);
    AtomicInteger moRequestPerSecond = new AtomicInteger(0);


    public MessageProcessing(
            MapLayer mapLayer, JedisCluster jedisCluster, Gateway gateway, CdrProcessor cdrProcessor, String redisMessageRetryQueue,
            String redisMessageList, ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap) {
        this.mapLayer = mapLayer;
        this.jedisCluster = jedisCluster;
        this.currentGateway = gateway;
        this.cdrProcessor = cdrProcessor;
        this.redisMessageRetryQueue = redisMessageRetryQueue;
        this.errorCodeMappingConcurrentHashMap = errorCodeMappingConcurrentHashMap;
        this.redisMessageList = redisMessageList;
        this.messageFactory = new MessageFactory(this.mapLayer);
        Thread.ofVirtual().name("SRI_Watcher").start(() -> new Watcher("SRI message send", sriRequestPerSecond, 1).startWatching());
        Thread.ofVirtual().name("SRI_RESP_Watcher").start(() -> new Watcher("SRI_RES message send", sriResponsePerSecond, 1).startWatching());
        Thread.ofVirtual().name("MT_Watcher").start(() -> new Watcher("MT message send", mtRequestPerSecond, 1).startWatching());
        Thread.ofVirtual().name("MO_Watcher").start(() -> new Watcher("MO message send", moRequestPerSecond, 1).startWatching());
    }


    /**
     * Send the message, send SRI if IMSI is NULL and if not send MT
     *
     * @param message The message that caused the timeout error.
     */
    public void sendMessage(MessageEvent message) {
        if (Objects.isNull(message.getImsi())) {
            sendRoutingInfoForSMRequest(message);
        } else {
            sendMtForwardSMRequest(message);
        }
    }

    public void processMessage(ChannelMessage channelMessage) {
        String messageType = (String) channelMessage.getParameter(Constants.MESSAGE_TYPE);
        switch (MAPMessageType.valueOf(messageType)) {
            case sendRoutingInfoForSM_Response:
                SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = (SendRoutingInfoForSMResponse) channelMessage.getParameter(Constants.MESSAGE);
                this.processSendRoutingInfoForSmResponse(sendRoutingInfoForSMResponse);
                break;

            case MAPMessageType.mtForwardSM_Response:
                MtForwardShortMessageResponse mtForwardShortMessageResponse = (MtForwardShortMessageResponse) channelMessage.getParameter(Constants.MESSAGE);
                this.processMtForwardSMResponse(mtForwardShortMessageResponse);
                break;

            case alertServiceCentre_Request, alertServiceCentreWithoutResult_Request:
                AlertServiceCentreRequest alertServiceCentreRequest = (AlertServiceCentreRequest) channelMessage.getParameter(Constants.MESSAGE);
                this.processAlertServiceCentreRequest(alertServiceCentreRequest);
                break;


            case MAPMessageType.moForwardSM_Request:
                MoForwardShortMessageRequest mtForwardShortMessageRequest = (MoForwardShortMessageRequest) channelMessage.getParameter(Constants.MESSAGE);
                this.processMoMessages(mtForwardShortMessageRequest);
                break;

            default:
                log.warn("No handler has been found for message type {}", messageType);
                break;
        }
    }

    public void processError(ChannelMessage channelMessage, String messageType) {
        MAPErrorMessage mapErrorMessage = (MAPErrorMessage) channelMessage.getParameter(Constants.MAP_ERROR_MESSAGE);
        String errorMsg = Ss7Utils.getMapErrorCodeToString(mapErrorMessage);
        log.debug("Handler error {}", errorMsg);
        MAPDialog mapDialog = (MAPDialog) channelMessage.getParameter(Constants.DIALOG);
        if (Objects.isNull(mapDialog)) {
            log.error("No dialog found for handle error");
            return;
        }

        if (Constants.ON_INVOKE_TIMEOUT.equals(messageType)) {
            MessageEvent message = removeMessageFromConcurrentHashMaps(mapDialog.getLocalDialogId());
            if (Objects.isNull(message)) {
                log.error("Timeout error occurred for dialog ID {}. No message found in the ConcurrentHashMaps.", mapDialog.getLocalDialogId());
                return;
            }
            this.processInvokeTimeoutError(message, mapErrorMessage);
            return;
        }

        this.processErrorComponent(mapDialog, mapErrorMessage);
    }

    private void sendRoutingInfoForSMRequest(MessageEvent message) {
        long localDialogId = 0;
        try {
            var mapDialogSms = this.messageFactory.createSendRoutingInfoForSMRequestFromMessageEvent(message);
            localDialogId = mapDialogSms.getLocalDialogId();
            messageSriConcurrentHashMap.put(localDialogId, message);
            checkCongestion();
            mapDialogSms.send();
            sriRequestPerSecond.incrementAndGet();
            this.processCdr(message, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SENT, "", false);

        } catch (Exception ex) {
            log.error("Error on sendRoutingInfoForSMRequest {}", ex.getMessage(), ex);
            messageSriConcurrentHashMap.remove(localDialogId);
            this.processCdr(message, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.FAILED, "ERROR ON SEND SRI", true);
        }
    }

    private void processSendRoutingInfoForSmResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse) {
        sriResponsePerSecond.incrementAndGet();
        MessageEvent message = messageSriConcurrentHashMap.remove(sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId());
        if (Objects.isNull(message)) {
            log.error("No message on messageSriConcurrentHashMap using DialogId -> {}", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId());
            return;
        }
        IMSI imsi = sendRoutingInfoForSMResponse.getIMSI();
        LocationInfoWithLMSI locationInfoWithLMSI = sendRoutingInfoForSMResponse.getLocationInfoWithLMSI();
        AddressString networkNodeNumber = locationInfoWithLMSI.getNetworkNodeNumber();
        message.setImsi(imsi.getData());
        message.setNetworkNodeNumber(networkNodeNumber.getAddress());
        message.setNetworkNodeNumberNatureOfAddress((int) CustomTypeOfNumber.fromPrimitive(networkNodeNumber.getAddressNature()).getSmscValue().value());
        message.setNetworkNodeNumberNumberingPlan((int) CustomNumberingPlanIndicator.fromPrimitive(networkNodeNumber.getNumberingPlan()).getSmscValue().value());

        if (message.isDropMapSri()) {
            this.processActionDropMapSri(message, networkNodeNumber);
            return;
        }

        if (message.isCheckSriResponse()) {
            message.setSriResponse(true);
            message.setOriginNetworkId(message.getDestNetworkId());
            this.putMessageOnSpecificList(redisMessageList, message);
            return;
        }

        if (message.getNetworkIdToMapSri() == -1 || message.getNetworkIdToMapSri() == 0) { // Send MT Forward SM to the same network, 0 mean is a DLR
            if (message.getMessageParts() != null) {
                message.getMessageParts().forEach(msgPart -> {
                    var messageEvent = new MessageEvent().clone(message);
                    messageEvent.setMessageId(msgPart.getMessageId());
                    messageEvent.setShortMessage(msgPart.getShortMessage());
                    messageEvent.setMsgReferenceNumber(msgPart.getMsgReferenceNumber());
                    messageEvent.setTotalSegment(msgPart.getTotalSegment());
                    messageEvent.setSegmentSequence(msgPart.getSegmentSequence());
                    messageEvent.setUdhJson(msgPart.getUdhJson());
                    messageEvent.setOptionalParameters(msgPart.getOptionalParameters());
                    this.sendMtForwardSMRequest(messageEvent);
                });
                return;
            }
            this.sendMtForwardSMRequest(message);
            return;
        }

        int networkIdToReroute = message.getNetworkIdToMapSri();
        message.setDestNetworkId(networkIdToReroute);
        this.putMessageOnNetworkIdList(networkIdToReroute, message);
    }

    private void sendMtForwardSMRequest(MessageEvent message) {
        var cdrMessageType = (message.isDlr()) ? UtilsEnum.MessageType.DELIVER : UtilsEnum.MessageType.MESSAGE;
        long localDialogId = 0;
        try {
            var mapDialogSms = messageFactory.createMtForwardSMRequestFromMessageEvent(message);
            localDialogId = mapDialogSms.getLocalDialogId();
            messageMtConcurrentHashMap.put(localDialogId, message);
            mapDialogSms.send();
            mtRequestPerSecond.incrementAndGet();
            this.processCdr(message, cdrMessageType, UtilsEnum.CdrStatus.SENT, "", false);
        } catch (Exception ex) {
            log.error("Error on sendMtForwardSMRequest {}", ex.getMessage(), ex);
            messageMtConcurrentHashMap.remove(localDialogId);
            this.processCdr(message, cdrMessageType, UtilsEnum.CdrStatus.FAILED, "ERROR ON SEND MT", false);
        }
    }

    private void processMtForwardSMResponse(MtForwardShortMessageResponse mtForwardShortMessageResponse) {
        long localDialogId = mtForwardShortMessageResponse.getMAPDialog().getLocalDialogId();
        MessageEvent sent = messageMtConcurrentHashMap.remove(localDialogId);
        if (Objects.isNull(sent)) {
            log.warn("No Message found for localId {}", localDialogId);
            return;
        }
        this.messageFactory.setSccpFieldsToMessage(sent, mtForwardShortMessageResponse.getMAPDialog());
        var messageTypeCdr = UtilsEnum.MessageType.MESSAGE;
        if (sent.isDlr()) {
            messageTypeCdr = UtilsEnum.MessageType.DELIVER;
        }
        this.processCdr(sent, messageTypeCdr, UtilsEnum.CdrStatus.SENT, "", true);
        if ((!sent.isDlr()) &&  (sent.getRegisteredDelivery() == RequestDelivery.REQUEST_DLR.getValue()) ) {
            this.prepareAndSendDlr(sent, null, null);
        }
    }

    private void processMoMessages(MoForwardShortMessageRequest moForwardShortMessageRequestIndication) {
        try {
            long invokeId = moForwardShortMessageRequestIndication.getInvokeId();
            MAPDialogSms mapDialogSms = moForwardShortMessageRequestIndication.getMAPDialog();
            ReturnResultLast returnResultLast = new ReturnResultLastImpl();
            returnResultLast.setInvokeId(invokeId);
            SmsSignalInfo smsSignalInfo = moForwardShortMessageRequestIndication.getSM_RP_UI();
            SmsTpdu smsTpduDecoded = smsSignalInfo.decodeTpdu(true);

            Map<SmsTpduType, Consumer<SmsTpdu>> smsTpduProcessors = Map.of(
                    SmsTpduType.SMS_SUBMIT, smsTpdu -> {
                        SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
                        log.debug("Received SMS_SUBMIT = {}", smsSubmitTpdu);
                        this.processMoSmsSubmit(moForwardShortMessageRequestIndication, smsSubmitTpdu);
                    },
                    SmsTpduType.SMS_DELIVER_REPORT, smsTpdu -> log.warn("Received SMS_DELIVER_REPORT this message will not be processed {}", smsTpdu),
                    SmsTpduType.SMS_COMMAND, smsTpdu -> log.warn("Received SMS_COMMAND this message will not be processed {}", smsTpdu)
            );

            Optional.ofNullable(smsTpduProcessors.get(smsTpduDecoded.getSmsTpduType()))
                    .orElse(tpdu -> log.warn("Received unknown SMS TpduType = {}", tpdu))
                    .accept(smsTpduDecoded);

            mapDialogSms.sendReturnResultLastComponent(returnResultLast);
            mapDialogSms.close(false);

        } catch (MAPException e) {
            log.error("Error on handler MoMessages ", e);
        }
    }


    private void processMoSmsSubmit(MoForwardShortMessageRequest moForwardShortMessageRequestIndication, SmsSubmitTpdu smsSubmitTpdu) {
        DataCodingScheme dataCodingScheme = smsSubmitTpdu.getDataCodingScheme();
        if (Ss7Utils.checkDataCodingSchemeSupport(dataCodingScheme.getCode())) {
            MessageEvent messageMo = messageFactory.createMessageEventFromMoForwardShortMessageRequest(moForwardShortMessageRequestIndication, smsSubmitTpdu);
            messageMo.setOriginNetworkId(this.currentGateway.getNetworkId());
            this.moRequestPerSecond.incrementAndGet();
            this.processCdr(messageMo, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.RECEIVED, "", false);
            this.putMessageOnSpecificList(redisMessageList, messageMo);
        } else {
            log.error("DataCodingScheme: {} is not sported only GSM7 and USC2 are supported.",
                    dataCodingScheme.getCharacterSet().name() + "-" + dataCodingScheme.getCode());
        }
    }

    private void processAlertServiceCentreRequest(AlertServiceCentreRequest alertServiceCentreRequest) {
        try {
            String msisdn = alertServiceCentreRequest.getMsisdn().getAddress();
            MAPDialogSms mapDialogSms = alertServiceCentreRequest.getMAPDialog();
            ReturnResultLast returnResultLast = new ReturnResultLastImpl();
            returnResultLast.setInvokeId(alertServiceCentreRequest.getInvokeId());
            mapDialogSms.sendReturnResultLastComponent(returnResultLast);
            mapDialogSms.close(false);
            var key = msisdn.concat(ABSENT_SUBSCRIBER_HASH_NAME);
            var hashMessage = jedisCluster.hgetAll(key);
            if (hashMessage.isEmpty()) {
                log.debug("Get Alert Service Center Request for MSISDN {}, but there are no pending messages", msisdn);
                return;
            }

            hashMessage.forEach((keyHash, value) -> {
                try {
                    log.debug("Processing message {}", value);
                    MessageEvent message = Converter.stringToObject(value, MessageEvent.class);
                    executorService.submit(() -> this.sendMessage(message));
                    jedisCluster.hdel(key, keyHash);
                } catch (Exception e) {
                    log.error("Error on process message {} for Alert Service Center", keyHash);
                }
            });
        } catch (Exception ex) {
            log.error("Error on AlertServiceCentreRequest {}", ex.getMessage(), ex);
        }
    }


    private void processActionDropMapSri(MessageEvent message, AddressString networkNodeNumber) {
        String extraString = " imsi:" +
                message.getImsi() +
                " nnn_digits:" +
                message.getNetworkNodeNumber() +
                " nnn_an:" +
                networkNodeNumber.getAddressNature().getIndicator() +
                " nnn_np:" +
                networkNodeNumber.getNumberingPlan().getIndicator();

        this.prepareAndSendDlr(message, null, extraString);
        log.info("Create DLR due drop SRI message: {}", message.getMessageId());
    }

    private void prepareAndSendDlr(MessageEvent sent, Integer errorCode, String extraInformation) {
        if (Objects.nonNull(sent)) {
            log.info("Prepare and send DLR for message {}", sent.getMessageId());
            ErrorCodeMapping errorCodeMapping = Objects.nonNull(errorCode) ? this.getErrorCodeMapping(errorCode) : null;
            var dlr = messageFactory.createDeliveryReceiptMessage(sent, errorCodeMapping, extraInformation);
            processCdr(dlr, UtilsEnum.MessageType.DELIVER, UtilsEnum.CdrStatus.ENQUEUE, "", false);
            switch (sent.getOriginProtocol().toUpperCase()) {
                case "HTTP" -> this.putMessageOnSpecificList("http_dlr", dlr);
                case "SMPP" -> {
                    if ("SP".equalsIgnoreCase(sent.getOriginNetworkType())) {
                        this.putMessageOnSpecificList("smpp_dlr", dlr);
                        return;
                    }
                    dlr.setDlr(false);
                    this.putMessageOnSpecificList(dlr.getDestNetworkId() + "_smpp_message", dlr);
                }
                case "SS7" -> this.putMessageOnNetworkIdList(dlr.getDestNetworkId(), dlr);
                default -> log.error("No protocol found for send DLR for message {}", sent.getMessageId());
            }
        }
    }

    /**
     * Handles timeout error when invoking a message, attempting to resend the message to the same network.
     *
     * @param message         The message that caused the timeout error.
     * @param mapErrorMessage The error message associated with the timeout.
     */
    private void processInvokeTimeoutError(MessageEvent message, MAPErrorMessage mapErrorMessage) {
        log.warn("The error received is onInvokeTimeout, we will try to send the message again to the same networkId");
        processCdr(message,
                UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.CdrStatus.FAILED,
                Ss7Utils.getMapErrorCodeToString(mapErrorMessage),
                true);
        executorService.submit(() -> sendToRetry(mapErrorMessage, message));
    }

    private void processErrorComponent(MAPDialog mapDialog, MAPErrorMessage mapErrorMessage) {
        MAPApplicationContextName mapApplicationContextName = mapDialog.getApplicationContext().getApplicationContextName();
        if ((mapApplicationContextName == MAPApplicationContextName.shortMsgMTRelayContext) || (mapApplicationContextName == MAPApplicationContextName.shortMsgGatewayContext)) {
            MessageEvent message = this.removeMessageFromConcurrentHashMaps(mapDialog.getLocalDialogId());
            if (message == null) {
                log.error("No message found for dialog ID {} in the ConcurrentHashMaps", mapDialog.getLocalDialogId());
                return;
            }

            this.processCdr(message,
                    UtilsEnum.MessageType.MESSAGE,
                    UtilsEnum.CdrStatus.FAILED,
                    Ss7Utils.getMapErrorCodeToString(mapErrorMessage),
                    true);

            if (this.isPermanentError(mapErrorMessage)) {
                log.warn("Permanent error on MTForwardSMRequest has been received for message {}, we will try to reroute the message to another network id defined on the message as networkIdToPermanentFailure", message.getMessageId());
                this.processPermanentError(message, Math.toIntExact(mapErrorMessage.getErrorCode()));
                return;
            }

            this.processTemporaryError(message, mapErrorMessage);
        }
    }

    private void processPermanentError(MessageEvent message, int errorCode) {
        Integer networkIdToReroute = message.getNetworkIdToPermanentFailure() > 0 ? message.getNetworkIdToPermanentFailure() : null;
        if (Objects.isNull(networkIdToReroute)) {
            log.warn("No networkIdToReroute on message for permanent error");
            prepareAndSendDlr(message, errorCode, null);
            return;
        }
        message.setDestNetworkId(networkIdToReroute);
        this.putMessageOnNetworkIdList(networkIdToReroute, message);
    }

    private void processTemporaryError(MessageEvent message, MAPErrorMessage mapErrorMessage) {
        Integer networkIdToRerouteTemp = message.getNetworkIdTempFailure() > 0 ? message.getNetworkIdToPermanentFailure() : null;
        if (Objects.nonNull(networkIdToRerouteTemp)) {
            log.warn("Temporary error on MTForwardSMRequest has been received for message {}, we will try to reroute the message to another network id defined on the message as networkIdToRerouteTemp", message.getMessageId());
            message.setDestNetworkId(networkIdToRerouteTemp);
            this.putMessageOnNetworkIdList(networkIdToRerouteTemp, message);
        } else {
            log.warn("The error received is temporary and no networkIdToRerouteTemp is defined on the message, we will try to send the message again to the same networkId");
            boolean isAbsentSubscriber = isAbsentSubscriberError(mapErrorMessage);
            if (message.getValidityPeriod() > 0 && !message.isLastRetry())
                this.sendReportSMDeliveryStatusRequest(message, isAbsentSubscriber);
            executorService.submit(() -> this.sendToRetry(mapErrorMessage, message));
        }
    }

    private void sendReportSMDeliveryStatusRequest(MessageEvent message, boolean isAbsentSubscriber) {
        try {
            MAPDialogSms mapDialogSms = messageFactory.createReportSMDeliveryStatusRequestFromMessageEvent(message, isAbsentSubscriber);
            mapDialogSms.send();
        } catch (Exception ex) {
            log.error("Error on handler prepareForRetry {}", ex.getMessage(), ex);
        }
    }

    private void sendToRetry(MAPErrorMessage mapErrorMessage, MessageEvent message) {
        try {
            log.warn("Starting auto retry validation process for message_id {}", message.getMessageId());
            message.setRetryNumber(Objects.isNull(message.getRetryNumber()) ? 1 : message.getRetryNumber() + 1);
            message.setNetworkNotifyError(true);
            message.setRetry(true);

            if (message.getValidityPeriod() == 0) {
                log.warn("The message with messageId {} won't be retried because the validity period is 0", message.getMessageId());
                this.prepareAndSendDlr(message, Math.toIntExact(mapErrorMessage.getErrorCode()), null);
                return;
            }

            if (message.isLastRetry()) {
                log.warn("Is Last Retry for message {}, preparing and sending DLR from sendToRetryProcess", message.getId());
                this.prepareAndSendDlr(message, Math.toIntExact(mapErrorMessage.getErrorCode()), null);
            } else {
                log.warn("Retry number {} for message: {}", message.getRetryNumber(), message);
                processCdr(message,
                        UtilsEnum.MessageType.MESSAGE,
                        UtilsEnum.CdrStatus.RETRY,
                        "",
                        false);

                executorService.submit(() ->
                        this.putMessageOnHashTable(message.getDestinationAddr().concat(ABSENT_SUBSCRIBER_HASH_NAME),
                                message.getMessageId(), message.toString())
                );
                executorService.submit(() ->
                        this.putMessageOnSpecificList(redisMessageRetryQueue, message)
                );
            }

        } catch (Exception ex) {
            log.error("Error on sendToRetry {}", ex.getMessage(), ex);
        }
    }


    private MessageEvent removeMessageFromConcurrentHashMaps(long mapDialogId) {
        MessageEvent message = messageSriConcurrentHashMap.remove(mapDialogId);
        if (Objects.isNull(message)) {
            message = messageMtConcurrentHashMap.remove(mapDialogId);
        }
        return message;
    }

    private boolean isPermanentError(MAPErrorMessage mapErrorMessage) {
        return !mapErrorMessage.isEmAbsentSubscriberSM() && !mapErrorMessage.isEmSMDeliveryFailure();
    }

    private boolean isAbsentSubscriberError(MAPErrorMessage mapErrorMessage) {
        return mapErrorMessage.isEmAbsentSubscriberSM();
    }

    private void putMessageOnNetworkIdList(int networkId, MessageEvent message) {
        final String listName = networkId + "_ss7_message";
        this.jedisCluster.rpush(listName, message.toString());
    }

    private void putMessageOnSpecificList(String listName, MessageEvent message) {
        this.jedisCluster.rpush(listName, message.toString());
    }

    private void putMessageOnHashTable(String key, String hash, String value) {
        this.jedisCluster.hset(key, hash, value);
    }

    private void waitForCongestion() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.error("Error on sleep for congestion control");
            Thread.currentThread().interrupt();
        }
    }

    private void checkCongestion() {
        NetworkIdState networkIdState = this.mapLayer.getMapStack().getMAPProvider().getNetworkIdState(0);
        int executorCongestionLevel = this.mapLayer.getMapStack().getMAPProvider().getExecutorCongestionLevel();
        if (!(networkIdState == null || networkIdState.isAvailable() && networkIdState.getCongLevel() <= 0 && executorCongestionLevel <= 0)) {
            // congestion or unavailable
            log.warn("Outgoing congestion control: MAP: networkIdState {}, executorCongestionLevel {}", networkIdState, executorCongestionLevel);
            waitForCongestion();
        }
    }

    private ErrorCodeMapping getErrorCodeMapping(Integer errorCode) {
        List<ErrorCodeMapping> errorCodeMappingList = errorCodeMappingConcurrentHashMap.get(String.valueOf(this.currentGateway.getMnoId()));
        return Optional.ofNullable(errorCodeMappingList)
                .flatMap(list -> list.stream().filter(errorCodeMapping -> errorCodeMapping.getErrorCode() == errorCode).findFirst())
                .orElseGet(() -> {
                    log.warn("No error code mapping found for mno {} with error {}. using status {}",
                            currentGateway.getMnoId(), errorCode, DeliveryReceiptState.UNDELIV);
                    ErrorCodeMapping defaultMapping = new ErrorCodeMapping();
                    defaultMapping.setErrorCode(errorCode);
                    defaultMapping.setDeliveryErrorCode(errorCode);
                    defaultMapping.setDeliveryStatus("UNDELIV");
                    return defaultMapping;
                });
    }

    private void processCdr(MessageEvent message, UtilsEnum.MessageType messageType,
                            UtilsEnum.CdrStatus cdrStatus, String comment, boolean writeCdr) {
        cdrProcessor.putCdrDetailOnRedis(message.toCdrDetail(
                UtilsEnum.Module.SS7_CLIENT,
                messageType,
                cdrStatus,
                comment
        ));

        if (writeCdr)
            cdrProcessor.createCdr(message.getMessageId());
    }
}
