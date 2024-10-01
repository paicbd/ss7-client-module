package com.paicbd.module.ss7.layer.impl;

import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.dto.MessageEvent;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.primitives.LMSI;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.AddressField;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.restcomm.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserData;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;
import org.restcomm.protocols.ss7.map.primitives.AddressStringImpl;
import org.restcomm.protocols.ss7.map.primitives.IMSIImpl;
import org.restcomm.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.restcomm.protocols.ss7.map.primitives.LMSIImpl;
import org.restcomm.protocols.ss7.map.service.sms.AlertServiceCentreRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.restcomm.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.MtForwardShortMessageResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.SM_RP_DAImpl;
import org.restcomm.protocols.ss7.map.service.sms.SM_RP_OAImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.restcomm.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.restcomm.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsSubmitTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.SmsTpduImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.restcomm.protocols.ss7.map.smstpdu.UserDataImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ValidityEnhancedFormatDataImpl;
import org.restcomm.protocols.ss7.map.smstpdu.ValidityPeriodImpl;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;
import org.restcomm.protocols.ss7.tcap.asn.ReturnResultLastImpl;
import org.restcomm.protocols.ss7.tcap.asn.comp.ReturnResultLast;

import java.nio.charset.Charset;

public class MessageUtil {

    public static MessageEvent getMessageEvent() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setId(System.currentTimeMillis() + "-" + System.nanoTime());
        messageEvent.setMessageId(System.currentTimeMillis() + "-" + System.nanoTime());
        messageEvent.setDeliverSmServerId(System.currentTimeMillis() + "-" + System.nanoTime());
        messageEvent.setOriginNetworkId(1);
        messageEvent.setOriginProtocol("SMPP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestNetworkId(2);
        messageEvent.setDestProtocol("SS7");
        messageEvent.setDestNetworkType("GW");
        messageEvent.setDeliverSmId("10");
        messageEvent.setDataCoding(0);
        messageEvent.setShortMessage("Hello World");
        messageEvent.setGlobalTitleIndicator("GT0100");
        messageEvent.setTranslationType(0);
        messageEvent.setGlobalTitle("888888");
        messageEvent.setSmscSsn(8);
        messageEvent.setHlrSsn(6);
        messageEvent.setMscSsn(8);
        messageEvent.setMapVersion(3);
        messageEvent.setDestAddrTon(1);
        messageEvent.setDestAddrNpi(4);
        messageEvent.setDestinationAddr("22222222");
        messageEvent.setMsisdn("22222222");
        messageEvent.setAddressNatureMsisdn(4);
        messageEvent.setNumberingPlanMsisdn(1);
        messageEvent.setSourceAddr("33333333");
        messageEvent.setSourceAddrTon(1);
        messageEvent.setSourceAddrNpi(4);
        messageEvent.setImsi("748031234567890");
        messageEvent.setNetworkNodeNumber("598991900032");
        messageEvent.setNetworkNodeNumberNatureOfAddress(4);
        messageEvent.setNetworkNodeNumberNumberingPlan(1);
        messageEvent.setRegisteredDelivery(1);
        messageEvent.setCommandStatus(0);
        return messageEvent;
    }

    public static MoForwardShortMessageRequest createMoMessage(ValidityPeriodFormat validityPeriodFormat,
                                                               MapLayer mapLayer, int dataCoding) throws MAPException {


        MAPDialogSms mapDialogSms = createMapDialog(mapLayer, MAPApplicationContextName.shortMsgMORelayContext);

        AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "5989900123");
        SM_RP_DA smRpDa = new SM_RP_DAImpl(serviceCentreAddressDA);
        ISDNAddressString msisdn = mapLayer.getMapProvider().getMAPParameterFactory()
                .createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, "31628838002");
        SM_RP_OAImpl smRpOa = new SM_RP_OAImpl();
        smRpOa.setMsisdn(msisdn);

        boolean rejectDuplicates = true;
        boolean replyPathExists = false;
        boolean statusReportRequest = true;
        int messageReference = 100;

        AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber,
                NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "59899077937");
        ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(0);
        ValidityPeriod validityPeriod = getValidityPeriod(validityPeriodFormat);
        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dataCoding);
        UserDataHeader userDataHeader = new UserDataHeaderImpl();
        Charset gsm8Charset = Charset.defaultCharset();

        UserData userData = new UserDataImpl("SMS load test", dataCodingScheme, userDataHeader, gsm8Charset);

        SmsTpduImpl smsTpdu = new SmsSubmitTpduImpl(rejectDuplicates, replyPathExists, statusReportRequest, messageReference, destinationAddress,
                protocolIdentifier, validityPeriod, userData);
        SmsSignalInfo smsSignalInfo = new SmsSignalInfoImpl(smsTpdu, gsm8Charset);
        IMSI imsi = new IMSIImpl("124356871012345");

        var moForwardShortMessageRequestImpl = new MoForwardShortMessageRequestImpl(smRpDa, smRpOa, smsSignalInfo, null, imsi);
        moForwardShortMessageRequestImpl.setMAPDialog(mapDialogSms);
        moForwardShortMessageRequestImpl.setInvokeId(messageReference);
        return moForwardShortMessageRequestImpl;
    }


    private static ValidityPeriod getValidityPeriod(ValidityPeriodFormat validityPeriodFormat) {
        return switch (validityPeriodFormat) {
            case fieldPresentRelativeFormat -> new ValidityPeriodImpl(3);
            case fieldPresentAbsoluteFormat -> new ValidityPeriodImpl(Ss7Utils.getAbsoluteTimeStampImpl());
            case fieldPresentEnhancedFormat -> new ValidityPeriodImpl(new ValidityEnhancedFormatDataImpl(new byte[0]));
            default -> null;
        };
    }


    public static SendRoutingInfoForSMResponse createSriMessage(MapLayer mapLayer) throws MAPException {
        long invokeId = 100;
        MAPDialogSms mapDialogSms =createMapDialog(mapLayer, MAPApplicationContextName.shortMsgMTRelayContext);
        IMSI imsi = new IMSIImpl("748031234567890");
        ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                NumberingPlan.ISDN, "598991900032");
        byte[] lmsiByte = new byte[]{114, 2, (byte) 233, (byte) 140};
        LMSI lmsi = new LMSIImpl(lmsiByte);
        boolean gprsNodeIndicator = false;
        LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, lmsi, null,
                gprsNodeIndicator, null);
        mapDialogSms.setUserObject(invokeId);
        SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = new SendRoutingInfoForSMResponseImpl(
                imsi, locationInfoWithLMSI, null, null, null);
        sendRoutingInfoForSMResponse.setInvokeId(100);
        sendRoutingInfoForSMResponse.setMAPDialog(mapDialogSms);
        return sendRoutingInfoForSMResponse;
    }

    public static MtForwardShortMessageResponse createMtResponse(MapLayer mapLayer) throws MAPException {
        long invokeId = 100;
        MAPDialogSms mapDialogSms = createMapDialog(mapLayer, MAPApplicationContextName.shortMsgMTRelayContext);
        mapDialogSms.setUserObject(invokeId);
        ReturnResultLast returnResultLast = new ReturnResultLastImpl();
        returnResultLast.setInvokeId(invokeId);
        mapDialogSms.sendReturnResultLastComponent(returnResultLast);
        MtForwardShortMessageResponseImpl mtForwardShortMessageResponseIndication = new MtForwardShortMessageResponseImpl();
        mtForwardShortMessageResponseIndication.setInvokeId(invokeId);
        mtForwardShortMessageResponseIndication.setMAPDialog(mapDialogSms);
        mtForwardShortMessageResponseIndication.setReturnResultNotLast(false);
        return mtForwardShortMessageResponseIndication;
    }

    public static AlertServiceCentreRequest createAlertServiceCentreRequest(MapLayer mapLayer) throws MAPException {
        long invokeId = 100;
        var msisdn = new ISDNAddressStringImpl(
                AddressNature.getInstance(1),
                org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.getInstance(4),
                "88888888");
        MAPDialogSms mapDialogSms = createMapDialog(mapLayer, MAPApplicationContextName.shortMsgMTRelayContext);
        AddressString serviceCentreAddress = mapLayer.getMapProvider().getMAPParameterFactory().createAddressString(AddressNature.subscriber_number,
                NumberingPlan.national, "0011");
        AlertServiceCentreRequest alertServiceCentreRequest = new AlertServiceCentreRequestImpl(msisdn, serviceCentreAddress);
        alertServiceCentreRequest.setInvokeId(invokeId);
        alertServiceCentreRequest.setMAPDialog(mapDialogSms);
        return alertServiceCentreRequest;

    }

    public static MAPDialogSms createMapDialog(MapLayer mapLayer, MAPApplicationContextName mapApplicationContextName) throws MAPException {
        AddressString origRef = mapLayer.getMapProvider().getMAPParameterFactory()
                .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "12345");
        AddressString destRef = mapLayer.getMapProvider().getMAPParameterFactory()
                .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "67890");

        GlobalTitle globalTitleClientSccpAddress = Ss7Utils.getGlobalTitle("GT0100", 0,
                null, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, "22222");

        SccpAddress clientSccpAddress = Ss7Utils.convertToSccpAddress(globalTitleClientSccpAddress, 0, 8);

        GlobalTitle globalTitleServerSccpAddress = Ss7Utils.getGlobalTitle("GT0100", 0,
                null, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, "33333");
        SccpAddress serverSccpAddress = Ss7Utils.convertToSccpAddress(globalTitleServerSccpAddress, 0, 8);

        return mapLayer.getMapProvider().getMAPServiceSms().createNewDialog(MAPApplicationContext
                        .getInstance(mapApplicationContextName, MAPApplicationContextVersion.version3),
                clientSccpAddress, origRef, serverSccpAddress, destRef);

    }
}
