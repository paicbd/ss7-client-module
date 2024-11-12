package com.paicbd.module.utils;

import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.IMSI;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SM_RP_DA;
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
import org.restcomm.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.SM_RP_DAImpl;
import org.restcomm.protocols.ss7.map.service.sms.SM_RP_OAImpl;
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

import java.nio.charset.Charset;

public class MessageUtil {

    public static MoForwardShortMessageRequest createMoMessage(ValidityPeriodFormat validityPeriodFormat,
                                                               MapLayer mapLayer, int dataCoding) throws MAPException {


        MAPDialogSms mapDialogSms = createMapDialog(mapLayer);

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


    private static MAPDialogSms createMapDialog(MapLayer mapLayer) throws MAPException {
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
                        .getInstance(MAPApplicationContextName.shortMsgMORelayContext, MAPApplicationContextVersion.version3),
                clientSccpAddress, origRef, serverSccpAddress, destRef);

    }
}
