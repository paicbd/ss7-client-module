package com.paicbd.module.ss7;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.MapRoutingData;
import com.paicbd.module.dto.MessageTransferData;
import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.CustomNumberingPlanIndicator;
import com.paicbd.module.utils.CustomTypeOfNumber;
import com.paicbd.module.utils.MessageTransferType;
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
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorCode;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MWStatus;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
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
    private final ConcurrentMap<String, MessageEvent> messageEventConcurrentMap = new ConcurrentHashMap<>();

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
        if (message.isNetworkNotifyError()) {
            this.setMessageAsNetworkNotifyError(message);
            this.sendToRetry(0L, message);
            return;
        }
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

            case MAPMessageType.InformServiceCentre_Request:
                InformServiceCentreRequest informServiceCentreRequest = (InformServiceCentreRequest) channelMessage.getParameter(Constants.MESSAGE);
                this.processInformServiceCentreRequest(informServiceCentreRequest);
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
            MessageTransferData messageTransferData = (MessageTransferData) mapDialog.getUserObject();
            MessageEvent message = messageTransferData.getMessageEvent();
            if (Objects.isNull(message)) {
                log.error("Timeout error occurred for dialog ID {}. No message found in the ConcurrentHashMaps.", mapDialog.getLocalDialogId());
                return;
            }
            this.processInvokeTimeoutError(message, mapErrorMessage);
            return;
        }

        this.processErrorComponent(mapDialog, mapErrorMessage);
    }

    public void processDialog(ChannelMessage channelMessage) {
        MAPDialog mapDialog = (MAPDialog) channelMessage.getParameter(Constants.DIALOG);
        MessageTransferData messageTransferData = (MessageTransferData) mapDialog.getUserObject();
        if (Objects.isNull(messageTransferData)) {
            return;
        }
        MessageEvent messageEvent = messageTransferData.getMessageEvent();
        if (Objects.isNull(messageEvent)) {
            log.error("No message event on processMessageTransferTypeSendRoutingInfo using DialogId -> {}", mapDialog.getLocalDialogId());
            return;
        }

        if (MessageTransferType.SEND_ROUTING_INFO_FOR_SM.equals(messageTransferData.getMessageTransferType())) {
            String id = messageTransferData.getMessageEvent().getId();
            if (Objects.nonNull(messageEventConcurrentMap.remove(id))) {
                this.processDialogTypeSendRoutingInfo(messageTransferData);
            }
        }
    }

    private void sendRoutingInfoForSMRequest(MessageEvent message) {
        try {
            var mapDialogSms = this.messageFactory.createSendRoutingInfoForSMRequestFromMessageEvent(message);
            MapRoutingData mapRoutingData = new MapRoutingData();
            MessageTransferData messageTransferData = new MessageTransferData(MessageTransferType.SEND_ROUTING_INFO_FOR_SM, message, mapRoutingData);
            mapDialogSms.setUserObject(messageTransferData);
            checkCongestion();
            messageEventConcurrentMap.put(message.getId(), message);
            mapDialogSms.send();
            sriRequestPerSecond.incrementAndGet();
            this.processCdr(message, UtilsEnum.CdrStatus.SENT, "", false);
        } catch (Exception ex) {
            log.error("Error on sendRoutingInfoForSMRequest {}", ex.getMessage(), ex);
            this.processCdr(message, UtilsEnum.CdrStatus.FAILED, "ERROR ON SEND SRI DUE TO " + ex.getMessage(), true);
        }
    }

    private void processSendRoutingInfoForSmResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse) {
        MessageTransferData messageTransferData = (MessageTransferData) sendRoutingInfoForSMResponse.getMAPDialog().getUserObject();
        MapRoutingData mapRoutingData = messageTransferData.getMapRoutingData();
        mapRoutingData.setImsi(sendRoutingInfoForSMResponse.getIMSI());
        mapRoutingData.setMwdSet(sendRoutingInfoForSMResponse.getMwdSet());
        mapRoutingData.setIpSmGwGuidance(sendRoutingInfoForSMResponse.getIpSmGwGuidance());
        mapRoutingData.setLocationInfoWithLMSI(sendRoutingInfoForSMResponse.getLocationInfoWithLMSI());
        sriResponsePerSecond.incrementAndGet();
    }

    private void processInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreRequest) {
        MessageTransferData messageTransferData = (MessageTransferData) informServiceCentreRequest.getMAPDialog().getUserObject();
        MapRoutingData mapRoutingData = messageTransferData.getMapRoutingData();
        mapRoutingData.setInformServiceCenterData(true);
        mapRoutingData.setStoredMSISDN(informServiceCentreRequest.getStoredMSISDN());
        mapRoutingData.setMwStatus(informServiceCentreRequest.getMwStatus());
        mapRoutingData.setAbsentSubscriberDiagnosticSM(informServiceCentreRequest.getAbsentSubscriberDiagnosticSM());
        mapRoutingData.setAdditionalAbsentSubscriberDiagnosticSM(informServiceCentreRequest.getAdditionalAbsentSubscriberDiagnosticSM());
    }

    private void sendMtForwardSMRequest(MessageEvent message) {
        try {
            var mapDialogSms = messageFactory.createMtForwardSMRequestFromMessageEvent(message);
            MessageTransferData messageTransferData = new MessageTransferData(MessageTransferType.SEND_MT_FORWARD_SM, message, null);
            mapDialogSms.setUserObject(messageTransferData);
            mapDialogSms.send();
            mtRequestPerSecond.incrementAndGet();
            this.processCdr(message, UtilsEnum.CdrStatus.SENT, "", false);
        } catch (Exception ex) {
            log.error("Error on sendMtForwardSMRequest {}", ex.getMessage(), ex);
            this.processCdr(message, UtilsEnum.CdrStatus.FAILED, "ERROR ON SEND MT DUE TO " + ex.getMessage(), true);
        }
    }

    private void processMtForwardSMResponse(MtForwardShortMessageResponse mtForwardShortMessageResponse) {
        MessageTransferData messageTransferData = (MessageTransferData) mtForwardShortMessageResponse.getMAPDialog().getUserObject();
        MessageEvent sent = messageTransferData.getMessageEvent();
        if (Objects.isNull(sent)) {
            log.warn("No Message found for localId {}", mtForwardShortMessageResponse.getMAPDialog().getLocalDialogId());
            return;
        }
        this.messageFactory.setSccpFieldsToMessage(sent, mtForwardShortMessageResponse.getMAPDialog());
        this.processCdr(sent, UtilsEnum.CdrStatus.SENT, "", true);
        this.prepareAndSendDlr(sent, null, null);
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
            this.processCdr(messageMo, UtilsEnum.CdrStatus.RECEIVED, "", false);
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

            hashMessage.forEach((messageId, value) -> {
                try {
                    log.debug("Processing message {}", value);
                    MessageEvent message = Converter.stringToObject(value, MessageEvent.class);
                    //Setting Null
                    message.setImsi(null);
                    message.setNetworkNotifyError(false);
                    this.sendMessage(message);
                    jedisCluster.hdel(key, messageId);
                } catch (Exception e) {
                    log.error("Error on process message {} for Alert Service Center", messageId);
                }
            });
        } catch (Exception ex) {
            log.error("Error on AlertServiceCentreRequest {}", ex.getMessage(), ex);
        }
    }

    private void processDialogTypeSendRoutingInfo(MessageTransferData messageTransferData) {
        MapRoutingData mapRoutingData = messageTransferData.getMapRoutingData();
        MessageEvent message = messageTransferData.getMessageEvent();
        if (Objects.nonNull(mapRoutingData.getMwStatus())) {
            log.debug("Evaluating InformServiceCenter Message");
            MWStatus mwStatus = mapRoutingData.getMwStatus();
            if (mwStatus.getMnrfSet() || mwStatus.getMcefSet()) {
                boolean isValidMessage = message.getValidityPeriod() > 0 && !message.isLastRetry();
                if (isValidMessage) {
                    SMDeliveryOutcome smDeliveryOutcome =
                            mwStatus.getMnrfSet() ? SMDeliveryOutcome.absentSubscriber : SMDeliveryOutcome.memoryCapacityExceeded;
                    this.setMessageAsNetworkNotifyError(message);
                    this.sendReportSMDeliveryStatusRequest(message, smDeliveryOutcome);
                }
                this.sendToRetry(0L, message);
                return;
            }
        }
        this.processSendRoutingForSmResponseAction(mapRoutingData, message);
    }


    private void processSendRoutingForSmResponseAction(MapRoutingData mapRoutingData, MessageEvent message) {
        IMSI imsi = mapRoutingData.getImsi();
        LocationInfoWithLMSI locationInfoWithLMSI = mapRoutingData.getLocationInfoWithLMSI();
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
        //Removing the message from the absent_subscriber_hash table
        if (sent.isNetworkNotifyError()) {
            var key = sent.getMsisdn().concat(ABSENT_SUBSCRIBER_HASH_NAME);
            log.debug("Removing from hash {} for message id {}", key, sent.getMessageId());
            jedisCluster.hdel(key, sent.getMessageId());
        }

        if (!sent.isDlr() && sent.getRegisteredDelivery() == RequestDelivery.REQUEST_DLR.getValue()) {
            log.info("Prepare and send DLR for message {}", sent.getMessageId());
            ErrorCodeMapping errorCodeMapping = Objects.nonNull(errorCode) ? this.getErrorCodeMapping(errorCode) : null;
            var dlr = messageFactory.createDeliveryReceiptMessage(sent, errorCodeMapping, extraInformation);
            processCdr(dlr, UtilsEnum.CdrStatus.ENQUEUE, "", false);
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
        messageEventConcurrentMap.remove(message.getId());
        processCdr(message,
                UtilsEnum.CdrStatus.FAILED,
                Ss7Utils.getMapErrorCodeToString(mapErrorMessage),
                true);
        this.sendToRetry(mapErrorMessage.getErrorCode(), message);
    }

    private void processErrorComponent(MAPDialog mapDialog, MAPErrorMessage mapErrorMessage) {
        MAPApplicationContextName mapApplicationContextName = mapDialog.getApplicationContext().getApplicationContextName();
        if ((mapApplicationContextName == MAPApplicationContextName.shortMsgMTRelayContext) || (mapApplicationContextName == MAPApplicationContextName.shortMsgGatewayContext)) {
            MessageTransferData messageTransferData = (MessageTransferData) mapDialog.getUserObject();
            MessageEvent message = messageTransferData.getMessageEvent();
            if (message == null) {
                log.error("No message found for dialog ID {} in the ConcurrentHashMaps", mapDialog.getLocalDialogId());
                return;
            }
            messageEventConcurrentMap.remove(message.getId());
            this.processCdr(message,
                    UtilsEnum.CdrStatus.FAILED,
                    Ss7Utils.getMapErrorCodeToString(mapErrorMessage),
                    true);

            if (this.isTempError(mapErrorMessage)) {
                this.processTemporaryError(message, mapErrorMessage);
                return;
            }
            this.processPermanentError(message, mapErrorMessage.getErrorCode().intValue());

        }
    }

    private void processPermanentError(MessageEvent message, int errorCode) {
        log.warn("Permanent error has been received for message {}, we will try to reroute the message to another network id defined on the message as networkIdToPermanentFailure", message.getMessageId());
        int newDestinationNetworkId = message.getNetworkIdToPermanentFailure();
        if (newDestinationNetworkId > 0) {
            message.setNetworkNotifyError(false);
            message.setDestNetworkId(newDestinationNetworkId);
            this.putMessageOnNetworkIdList(newDestinationNetworkId, message);
            return;
        }
        log.warn("No networkIdToReroute on message for permanent error");
        prepareAndSendDlr(message, errorCode, null);
    }

    private void processTemporaryError(MessageEvent message, MAPErrorMessage mapErrorMessage) {
        log.warn("Temporary error has been received for message {}, we will try to reroute the message to another network id defined on the message as networkIdTempFailure", message.getMessageId());
        int newDestinationNetworkId = message.getNetworkIdTempFailure();
        if (newDestinationNetworkId > 0) {
            log.warn("No networkIdToReroute on message for temporary error");
            message.setNetworkNotifyError(false);
            message.setDestNetworkId(newDestinationNetworkId);
            this.putMessageOnNetworkIdList(newDestinationNetworkId, message);
        } else {
            log.warn("The error received is temporary and no networkIdToRerouteTemp is defined on the message, we will try to send the message again to the same networkId");
            boolean isNetworkNotifyError = this.isReportSMDeliveryStatusRequired(mapErrorMessage);
            boolean isValidMessage = message.getValidityPeriod() > 0 && !message.isLastRetry();
            if (isNetworkNotifyError && isValidMessage) {
                SMDeliveryOutcome smDeliveryOutcome =
                        mapErrorMessage.isEmAbsentSubscriberSM() ? SMDeliveryOutcome.absentSubscriber : SMDeliveryOutcome.memoryCapacityExceeded;
                log.warn("Sending a ReportSMDeliveryStatusRequest with DeliveryOutcome {}for message Id {}", smDeliveryOutcome.name(), message.getMessageId());
                this.setMessageAsNetworkNotifyError(message);
                this.sendReportSMDeliveryStatusRequest(message, smDeliveryOutcome);
            }
            this.sendToRetry(mapErrorMessage.getErrorCode(), message);
        }
    }

    private void sendReportSMDeliveryStatusRequest(MessageEvent message, SMDeliveryOutcome smDeliveryOutcome) {
        try {
            MAPDialogSms mapDialogSms = messageFactory.createReportSMDeliveryStatusRequestFromMessageEvent(message, smDeliveryOutcome);
            mapDialogSms.send();
        } catch (Exception ex) {
            log.error("Error on handler prepareForRetry {}", ex.getMessage(), ex);
        }
    }

    private void sendToRetry(long mapErrorCode, MessageEvent message) {
        try {
            log.warn("Starting auto retry validation process for message_id {}", message.getMessageId());

            if (message.getValidityPeriod() == 0) {
                log.warn("The message with messageId {} won't be retried because the validity period is 0", message.getMessageId());
                this.prepareAndSendDlr(message, (int) mapErrorCode, null);
                return;
            }

            if (message.isLastRetry()) {
                log.warn("Is Last Retry for message {}, preparing and sending DLR from sendToRetryProcess", message.getId());
                this.prepareAndSendDlr(message, (int) mapErrorCode, null);
                return;
            }

            processCdr(message,
                    UtilsEnum.CdrStatus.RETRY,
                    "",
                    false);

            if (Objects.isNull(message.getRetryNumber()) || message.getRetryNumber() >= 0) {
                message.setRetry(true);
                message.setRetryNumber(Objects.isNull(message.getRetryNumber()) ? 1 : message.getRetryNumber() + 1);
                log.warn("Adding retry number {} for message: {}", message.getRetryNumber(), message);
                this.putMessageOnSpecificList(redisMessageRetryQueue, message);
            }
        } catch (Exception ex) {
            log.error("Error on sendToRetry {}", ex.getMessage(), ex);
        }
    }

    private boolean isTempError(MAPErrorMessage mapErrorMessage) {
        return switch (mapErrorMessage.getErrorCode().intValue()) {
            case MAPErrorCode.smDeliveryFailure -> {
                var mapErrorMessageSmDeliveryFailure = mapErrorMessage.getEmSMDeliveryFailure();
                yield SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded.equals(mapErrorMessageSmDeliveryFailure.getSMEnumeratedDeliveryFailureCause())
                        || SMEnumeratedDeliveryFailureCause.equipmentProtocolError.equals(mapErrorMessageSmDeliveryFailure.getSMEnumeratedDeliveryFailureCause());
            }

            case MAPErrorCode.absentSubscriber, MAPErrorCode.absentSubscriberSM, MAPErrorCode.subscriberBusyForMTSMS,
                 MAPErrorCode.busySubscriber, MAPErrorCode.systemFailure -> true;
            default -> false;
        };
    }

    private boolean isReportSMDeliveryStatusRequired(MAPErrorMessage mapErrorMessage) {
        return switch (mapErrorMessage.getErrorCode().intValue()) {
            case MAPErrorCode.smDeliveryFailure -> {
                var mapErrorMessageSmDeliveryFailure = mapErrorMessage.getEmSMDeliveryFailure();
                yield SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded.equals(mapErrorMessageSmDeliveryFailure.getSMEnumeratedDeliveryFailureCause());
            }

            case MAPErrorCode.absentSubscriber, MAPErrorCode.absentSubscriberSM -> true;
            default -> false;
        };
    }

    private void setMessageAsNetworkNotifyError(MessageEvent message) {
        message.setNetworkNotifyError(true);
        this.putMessageOnHashTable(message.getMsisdn().concat(ABSENT_SUBSCRIBER_HASH_NAME),
                message.getMessageId(), message.toString());
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

    private void processCdr(MessageEvent message, UtilsEnum.CdrStatus cdrStatus, String comment, boolean writeCdr) {
        cdrProcessor.putCdrDetailOnRedis(message.toCdrDetail(
                UtilsEnum.Module.SS7_CLIENT,
                (message.isDlr()) ? UtilsEnum.MessageType.DELIVER : UtilsEnum.MessageType.MESSAGE,
                cdrStatus,
                comment
        ));

        if (writeCdr)
            cdrProcessor.createCdr(message.getMessageId());
    }
}
