package com.paicbd.module.ss7;

import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.utils.CustomNumberingPlanIndicator;
import com.paicbd.module.utils.CustomTypeOfNumber;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.RequestDelivery;
import com.paicbd.smsc.utils.UtilsEnum;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.util.DeliveryReceiptState;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.restcomm.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.restcomm.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.restcomm.protocols.ss7.map.api.smstpdu.AddressField;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsStatusReportTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.StatusReportQualifier;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserData;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;
import org.restcomm.protocols.ss7.map.primitives.AddressStringImpl;
import org.restcomm.protocols.ss7.map.primitives.IMSIImpl;
import org.restcomm.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.restcomm.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataImpl;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.paicbd.module.utils.Constants.GW;
import static com.paicbd.module.utils.Constants.PROTOCOL;

@Slf4j
public class MessageFactory {

    private static final int GSM7 = 0;
    private static final int UCS2 = 8;

    private final MapLayer mapLayer;
    private final MAPSmsTpduParameterFactory mapSmsTpduParameterFactory;

    public MessageFactory(MapLayer mapLayer) {
        this.mapLayer = mapLayer;
        this.mapSmsTpduParameterFactory = this.mapLayer.getMapProvider().getMAPSmsTpduParameterFactory();
    }

    public MAPDialogSms createSendRoutingInfoForSMRequestFromMessageEvent(MessageEvent message) throws MAPException {
        boolean smRpPri = true;
        MAPDialogSms mapDialogSms = this.createDialogForRequestToHLR(message);
        AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, message.getGlobalTitle());
        ISDNAddressString msisdn = Ss7Utils.getMsisdn(message);
        mapDialogSms.addSendRoutingInfoForSMRequest(msisdn, smRpPri, serviceCentreAddress, null, false, null, null,
                null, false, null, false, false, null, null);
        this.setSccpFieldsToMessage(message, mapDialogSms);
        this.logMapMessage("SendRoutingInfoForSMRequest", mapDialogSms);
        return mapDialogSms;
    }

    public MAPDialogSms createMtForwardSMRequestFromMessageEvent(MessageEvent message, MAPDialogSms mapDialogSms) throws MAPException {

        IMSI imsi = new IMSIImpl(message.getImsi());

        SccpAddress clientSccpAddress = this.getSmscSccpAddress(message);

        GlobalTitle globalTitleServerSccpAddress = Ss7Utils.getGlobalTitle(
                message.getGlobalTitleIndicator(),
                message.getTranslationType(),
                null,
                CustomNumberingPlanIndicator.fromSmsc(message.getNetworkNodeNumberNumberingPlan().byteValue()).getIndicatorValue(),
                CustomTypeOfNumber.fromSmsc(message.getNetworkNodeNumberNatureOfAddress().byteValue()).getIndicatorValue(),
                message.getNetworkNodeNumber()
        );
        SccpAddress serverSccpAddress = Ss7Utils.convertToSccpAddress(globalTitleServerSccpAddress, 0, message.getMscSsn());

        MAPApplicationContextVersion mapAcnVersion = MAPApplicationContextVersion.getInstance(message.getMapVersion());
        MAPApplicationContextName mapAcn = MAPApplicationContextName.shortMsgMTRelayContext;
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(mapAcn, mapAcnVersion);
        if (Objects.isNull(mapDialogSms)) {
            mapDialogSms = this.mapLayer.getMapProvider().getMAPServiceSms().createNewDialog(mapAppContext, clientSccpAddress,
                    null, serverSccpAddress, null);
        }

        SM_RP_DA da = this.mapLayer.getMapProvider().getMAPParameterFactory().createSM_RP_DA(imsi);

        AddressString serviceCentreAddressOA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, message.getGlobalTitle());
        SM_RP_OA oa = this.mapLayer.getMapProvider().getMAPParameterFactory().createSM_RP_OA_ServiceCentreAddressOA(serviceCentreAddressOA);

        AddressField originatingAddress = new AddressFieldImpl(
                CustomTypeOfNumber.fromSmsc(message.getSourceAddrTon().byteValue()).getSmsTpduValue(),
                CustomNumberingPlanIndicator.fromSmsc(message.getSourceAddrNpi().byteValue()).getSmsTpduValue(),
                message.getSourceAddr());

        AbsoluteTimeStamp serviceCentreTimeStamp = Ss7Utils.getAbsoluteTimeStampImpl();
        int dcsVal = message.getDataCoding() == UCS2 ? UCS2 : GSM7; // 0 = GSM7, 8 = UCS2
        DataCodingScheme dcs = new DataCodingSchemeImpl(dcsVal);
        UserDataHeader udh = null;
        Charset charsetEncoding = Charset.defaultCharset();

        if (Converter.hasValidValue(message.getUdhJson())) {
            udh = new UserDataHeaderImpl();
            UserDataHeaderElement concatenatedShortMessagesIdentifier = this.mapSmsTpduParameterFactory
                    .createConcatenatedShortMessagesIdentifier(Integer.parseInt(message.getMsgReferenceNumber()) > 255, Integer.parseInt(message.getMsgReferenceNumber()),
                            message.getTotalSegment(), message.getSegmentSequence());
            udh.addInformationElement(concatenatedShortMessagesIdentifier);
        }

        // evaluate Charset
        if (dcs.getCode() == 8) {
            charsetEncoding = StandardCharsets.UTF_16;
        }

        boolean moreMessagesToSend = false;
        if (Objects.nonNull(message.getTotalSegment())) {
            moreMessagesToSend = !Objects.equals(message.getTotalSegment(), message.getSegmentSequence());
        }
        boolean forwardedOrSpawned = false;
        boolean replyPathExists = false;
        boolean statusReportIndication = Objects.equals(message.getRegisteredDelivery(), RequestDelivery.REQUEST_DLR.getValue());
        SmsSignalInfo si;
        ProtocolIdentifier pi = new ProtocolIdentifierImpl(0);

        if (message.isDlr()) {
            //MT DLR
            long messageTime = Long.parseLong(message.getId().split("-")[0]);
            AbsoluteTimeStamp deliveryTimeStamp = Ss7Utils.epochUtcTimeToAbsoluteTimeStampImpl(messageTime);
            SmsStatusReportTpdu smsStatusReportTpdu = this.mapSmsTpduParameterFactory.createSmsStatusReportTpdu(moreMessagesToSend,
                    false, StatusReportQualifier.SmsSubmitResult, Integer.parseInt(message.getDeliverSmId()),
                    originatingAddress, serviceCentreTimeStamp, deliveryTimeStamp,
                    this.mapSmsTpduParameterFactory.createStatus(message.getCommandStatus()),
                    this.mapSmsTpduParameterFactory.createProtocolIdentifier(0), null);

            si = this.mapLayer.getMapProvider().getMAPParameterFactory()
                    .createSmsSignalInfo(smsStatusReportTpdu, charsetEncoding);
        } else {
            UserData userData = new UserDataImpl(message.getShortMessage(), dcs, udh, charsetEncoding);
            SmsDeliverTpdu tPdu = new SmsDeliverTpduImpl(moreMessagesToSend, forwardedOrSpawned,
                    replyPathExists, statusReportIndication, originatingAddress, pi,
                    serviceCentreTimeStamp, userData);
            si = this.mapLayer.getMapProvider().getMAPParameterFactory().createSmsSignalInfo(tPdu, charsetEncoding);
        }

        mapDialogSms.addMtForwardShortMessageRequest(da, oa, si, moreMessagesToSend, null);
        this.setSccpFieldsToMessage(message, mapDialogSms);
        this.logMapMessage("MtForwardSMRequest", mapDialogSms);
        return mapDialogSms;
    }

    public MAPDialogSms createReportSMDeliveryStatusRequestFromMessageEvent(MessageEvent message, SMDeliveryOutcome smDeliveryOutcome) throws MAPException {
        MAPDialogSms mapDialogSms = this.createDialogForRequestToHLR(message);
        AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, message.getGlobalTitle());
        ISDNAddressString msisdn = Ss7Utils.getMsisdn(message);
        mapDialogSms.addReportSMDeliveryStatusRequest(msisdn, serviceCentreAddress, smDeliveryOutcome, null,
                null, false, false, null, null);
        this.logMapMessage("ReportSMDeliveryStatusRequest", mapDialogSms);
        return mapDialogSms;
    }

    public MessageEvent createMessageEventFromMoForwardShortMessageRequest(
            MoForwardShortMessageRequest moForwardShortMessageRequestIndication, SmsSubmitTpdu smsSubmitTpdu) {
        MessageEvent messageMo = new MessageEvent();
        this.logMapMessage("MoForwardShortMessageRequest", moForwardShortMessageRequestIndication.getMAPDialog());
        DataCodingScheme dataCodingScheme = smsSubmitTpdu.getDataCodingScheme();
        messageMo.setId(System.currentTimeMillis() + "-" + System.nanoTime());
        var messageId = smsSubmitTpdu.getMessageReference() + "";
        messageMo.setMessageId(messageId);
        messageMo.setParentId(messageId);
        messageMo.setEsmClass(0);
        messageMo.setMoMessage(true);
        ISDNAddressString originMsisdn = moForwardShortMessageRequestIndication.getSM_RP_OA().getMsisdn();
        messageMo.setSourceAddr(originMsisdn.getAddress());
        messageMo.setSourceAddrTon((int) CustomTypeOfNumber.fromPrimitive(originMsisdn.getAddressNature()).getSmscValue().value());
        messageMo.setSourceAddrNpi((int) CustomNumberingPlanIndicator.fromPrimitive(originMsisdn.getNumberingPlan()).getSmscValue().value());
        messageMo.setDataCoding(dataCodingScheme.getCode());
        UserData userData = smsSubmitTpdu.getUserData();
        SccpAddress localSccpAddress = moForwardShortMessageRequestIndication.getMAPDialog().getLocalAddress();
        messageMo.setOriginatorSccpAddress(localSccpAddress.getGlobalTitle().getDigits());
        setSccpFieldsToMessage(messageMo, moForwardShortMessageRequestIndication.getMAPDialog());
        try {
            userData.decode();
            String shortMessage = userData.getDecodedMessage();
            String destination = smsSubmitTpdu.getDestinationAddress().getAddressValue();
            int destinationTon = CustomTypeOfNumber.fromSmsTpdu(smsSubmitTpdu.getDestinationAddress().getTypeOfNumber()).getSmscValue().value();
            int destinationNpi = CustomNumberingPlanIndicator.fromSmsTpdu(smsSubmitTpdu.getDestinationAddress().getNumberingPlanIdentification()).getSmscValue().value();
            messageMo.setUdhi(smsSubmitTpdu.getUserDataHeaderIndicator() ? "1" : "0");
            messageMo.setDestinationAddr(destination);
            messageMo.setDestAddrTon(destinationTon);
            messageMo.setDestAddrNpi(destinationNpi);
            messageMo.setMsisdn(destination);
            messageMo.setAddressNatureMsisdn(destinationTon);
            messageMo.setNumberingPlanMsisdn(destinationNpi);
            messageMo.setRegisteredDelivery(smsSubmitTpdu.getStatusReportRequest() ? 1 : 0);
            messageMo.setOriginProtocol(PROTOCOL);
            messageMo.setOriginNetworkType(GW);
            messageMo.setShortMessage(shortMessage);
            messageMo.setSccpCalledPartyAddress(moForwardShortMessageRequestIndication.getMAPDialog()
                    .getLocalAddress().getGlobalTitle().getDigits());
            messageMo.setSccpCallingPartyAddress(moForwardShortMessageRequestIndication.getMAPDialog()
                    .getRemoteAddress().getGlobalTitle().getDigits());

            ValidityPeriod validityPeriod = smsSubmitTpdu.getValidityPeriod();
            ValidityPeriodFormat validityPeriodFormat = smsSubmitTpdu.getValidityPeriodFormat();

            if (Objects.nonNull(validityPeriod) && Objects.nonNull(validityPeriodFormat)) {
                switch (validityPeriodFormat) {
                    case fieldPresentRelativeFormat -> {
                        long secondsValidityPeriod = (long) (validityPeriod.getRelativeFormatHours() * 3600);
                        messageMo.setValidityPeriod(secondsValidityPeriod);
                    }
                    case fieldPresentAbsoluteFormat -> {
                        AbsoluteTimeStamp absoluteTimeStamp = validityPeriod.getAbsoluteFormatValue();
                        Calendar calendarDate = Ss7Utils.toCalendar(absoluteTimeStamp);
                        long timeInMillis = calendarDate.getTimeInMillis();
                        int atsTimeZoneOffset = absoluteTimeStamp.getTimeZone() * 15 * 60;
                        int localTimeZoneOffset = -calendarDate.getTimeZone().getOffset(timeInMillis) / 1000;
                        long timeZoneDifferenceInMillis = (localTimeZoneOffset - atsTimeZoneOffset) * 1000L;
                        long adjustedTimeInMillis = timeInMillis + timeZoneDifferenceInMillis;
                        Calendar adjustedCalendar = Calendar.getInstance();
                        adjustedCalendar.setTimeInMillis(adjustedTimeInMillis);
                        long differenceInMillis = adjustedCalendar.getTimeInMillis() - calendarDate.getTimeInMillis();
                        long differenceInSeconds = differenceInMillis / 1000;
                        messageMo.setValidityPeriod(differenceInSeconds);
                    }
                    default -> {
                        log.warn("Received unsupported ValidityPeriodFormat: {} - we set 80s", validityPeriodFormat);
                        messageMo.setValidityPeriod(80);
                    }
                }
            }
        } catch (MAPException e) {
            log.error("MO MAPException when decoding user data ", e);
        }
        return messageMo;
    }

    public MessageEvent createDeliveryReceiptMessage(MessageEvent messageEvent, ErrorCodeMapping errorCodeMapping, String extraInformation) {
        MessageEvent deliveryReceiptMessageEvent = new MessageEvent();
        DeliveryReceiptState deliveryReceiptState;
        DeliveryReceipt deliveryReceipt = new DeliveryReceipt(messageEvent.getParentId(), 1, 1,
                new Date(), new Date(), DeliveryReceiptState.DELIVRD, "000", "");
        deliveryReceiptMessageEvent.clone(messageEvent);
        if (errorCodeMapping != null) {
            deliveryReceipt.setSubmitted(1);
            deliveryReceipt.setDelivered(0);
            deliveryReceiptState = UtilsEnum.getDeliverReceiptState(errorCodeMapping.getDeliveryStatus());
            String error = errorCodeMapping.getDeliveryErrorCode() + "";
            deliveryReceipt.setFinalStatus(deliveryReceiptState);
            deliveryReceipt.setError(error);
            log.warn("Creating deliver_sm with status {} and error {} for submit_sm with id {}", deliveryReceiptState, error, messageEvent.getParentId());
        }

        String dlrMessage = Objects.isNull(extraInformation) ? deliveryReceipt.toString() : deliveryReceipt.toString().concat(extraInformation);
        deliveryReceiptMessageEvent.setId(System.currentTimeMillis() + "-" + System.nanoTime());
        deliveryReceiptMessageEvent.setRegisteredDelivery(0);
        deliveryReceiptMessageEvent.setDeliverSmId(messageEvent.getMessageId());
        deliveryReceiptMessageEvent.setDlr(true);
        deliveryReceiptMessageEvent.setImsi(null);
        deliveryReceiptMessageEvent.setShortMessage(dlrMessage);
        deliveryReceiptMessageEvent.setDelReceipt(dlrMessage);
        deliveryReceiptMessageEvent.setOriginNetworkId(messageEvent.getDestNetworkId());
        deliveryReceiptMessageEvent.setOriginProtocol(messageEvent.getDestProtocol());
        deliveryReceiptMessageEvent.setOriginNetworkType(messageEvent.getDestNetworkType());
        deliveryReceiptMessageEvent.setDestNetworkId(messageEvent.getOriginNetworkId());
        deliveryReceiptMessageEvent.setDestProtocol(messageEvent.getOriginProtocol());
        deliveryReceiptMessageEvent.setDestNetworkType(messageEvent.getOriginNetworkType());
        deliveryReceiptMessageEvent.setSourceAddrTon(messageEvent.getDestAddrTon());
        deliveryReceiptMessageEvent.setSourceAddrNpi(messageEvent.getDestAddrNpi());
        deliveryReceiptMessageEvent.setSourceAddr(messageEvent.getDestinationAddr());
        deliveryReceiptMessageEvent.setDestAddrTon(messageEvent.getSourceAddrTon());
        deliveryReceiptMessageEvent.setDestAddrNpi(messageEvent.getSourceAddrNpi());
        deliveryReceiptMessageEvent.setDestinationAddr(messageEvent.getSourceAddr());
        deliveryReceiptMessageEvent.setCheckSubmitSmResponse(false);
        deliveryReceiptMessageEvent.setEsmClass(null);
        deliveryReceiptMessageEvent.setMsisdn(deliveryReceiptMessageEvent.getDestinationAddr());
        deliveryReceiptMessageEvent.setAddressNatureMsisdn(deliveryReceiptMessageEvent.getDestAddrTon());
        deliveryReceiptMessageEvent.setNumberingPlanMsisdn(deliveryReceiptMessageEvent.getDestAddrNpi());
        deliveryReceiptMessageEvent.setStatus(deliveryReceipt.getFinalStatus().name());
        return deliveryReceiptMessageEvent;
    }

    private void logMapMessage(String messageType, MAPDialogSms mapDialogSms) {
        log.debug("MAP message {} with " +
                  "LocalAddress: {}, " +
                  "RemoteAddress: {}," +
                  "LocalDialogId: {}, " +
                  "RemoteDialogId: {}",
                messageType, mapDialogSms.getLocalAddress(), mapDialogSms.getRemoteAddress(), mapDialogSms.getLocalDialogId(), mapDialogSms.getRemoteDialogId());
    }

    private MAPDialogSms createDialogForRequestToHLR(MessageEvent message) throws MAPException {
        //Creating Calling Party Address
        SccpAddress clientSccpAddress = this.getSmscSccpAddress(message);
        //Creating Called Party Address
        SccpAddress serverSccpAddress = this.getHlrSccpAddress(message);

        return this.mapLayer.getMapProvider().getMAPServiceSms().createNewDialog(MAPApplicationContext
                        .getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.getInstance(message.getMapVersion())),
                clientSccpAddress, null, serverSccpAddress, null);
    }

    private SccpAddress getSmscSccpAddress(MessageEvent message) {
        GlobalTitle globalTitleClientSccpAddress = Ss7Utils.getGlobalTitle(
                message.getGlobalTitleIndicator(),
                message.getTranslationType(),
                null,
                org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY,
                NatureOfAddress.INTERNATIONAL,
                message.getGlobalTitle()
        );
        return Ss7Utils.convertToSccpAddress(globalTitleClientSccpAddress, 0, message.getSmscSsn());
    }

    private SccpAddress getHlrSccpAddress(MessageEvent message) {
        GlobalTitle globalTitleServerSccpAddress = Ss7Utils.getGlobalTitle(
                message.getGlobalTitleIndicator(),
                message.getTranslationType(),
                null,
                CustomNumberingPlanIndicator.fromSmsc(message.getDestAddrNpi().byteValue()).getIndicatorValue(),
                CustomTypeOfNumber.fromSmsc(message.getDestAddrTon().byteValue()).getIndicatorValue(),
                message.getDestinationAddr()
        );
        return Ss7Utils.convertToSccpAddress(globalTitleServerSccpAddress, 0, message.getHlrSsn());
    }

    public void setSccpFieldsToMessage(MessageEvent messageEvent, MAPDialogSms mapDialogSms) {
        messageEvent.setLocalDialogId(mapDialogSms.getLocalDialogId());
        messageEvent.setRemoteDialogId(mapDialogSms.getRemoteDialogId());
        SccpAddress localSccpAddress = mapDialogSms.getLocalAddress();
        SccpAddress remoteSccpAddress = mapDialogSms.getRemoteAddress();
        messageEvent.setSccpCallingPartyAddress(localSccpAddress.getGlobalTitle().getDigits());
        messageEvent.setSccpCallingPartyAddressSubSystemNumber(localSccpAddress.getSubsystemNumber());
        messageEvent.setSccpCallingPartyAddressPointCode(localSccpAddress.getSignalingPointCode());

        messageEvent.setSccpCalledPartyAddress(remoteSccpAddress.getGlobalTitle().getDigits());
        messageEvent.setSccpCalledPartyAddressSubSystemNumber(remoteSccpAddress.getSubsystemNumber());
        messageEvent.setSccpCalledPartyAddressPointCode(remoteSccpAddress.getSignalingPointCode());
    }
}
