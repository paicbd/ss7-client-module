package com.paicbd.module.utils;

import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.smsc.dto.MessageEvent;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.indicator.NumberingPlan;
import org.restcomm.protocols.ss7.indicator.RoutingIndicator;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.restcomm.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.restcomm.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.restcomm.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.restcomm.protocols.ss7.sccp.parameter.EncodingScheme;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;

import static com.paicbd.module.utils.Constants.errorToStringMap;

@Slf4j
public class Ss7Utils {

    @Generated
    private Ss7Utils() {
        throw new IllegalStateException("Utility Class");
    }

    @Generated
    public enum LayerType {
        SCTP, M3UA, SCCP, TCAP, MAP
    }
    @Generated
    public enum AssociationType {
        CLIENT, SERVER
    }


    public static GlobalTitle getGlobalTitle(String gtIndicator, int translationType, EncodingScheme encodingScheme,
                                             NumberingPlan numberingPlan, NatureOfAddress natureOfAddress, String digit) {
        log.debug("Creating GlobalTitle with : GlobalTitle: {}, EncodingScheme: {}, TranslationType: {}, " +
                        "NumberingPlan: {}, NatureOfAddress:{}", gtIndicator,
                encodingScheme != null ? encodingScheme.getType().toString() : "null",
                translationType, numberingPlan, natureOfAddress);
        ParameterFactoryImpl factory = new ParameterFactoryImpl();
        return switch (gtIndicator) {
            case "GT0001" -> factory.createGlobalTitle(digit, natureOfAddress);
            case "GT0010" -> factory.createGlobalTitle(digit, translationType);
            case "GT0011" -> factory.createGlobalTitle(digit, translationType, numberingPlan, encodingScheme);
            case "GT0100" -> factory.createGlobalTitle(digit, translationType, numberingPlan, encodingScheme, natureOfAddress);
            default -> factory.createGlobalTitle(digit);
        };
    }

    public static SccpAddress convertToSccpAddress(GlobalTitle globalTitle, int pointCode, int ssn) {
        return new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, globalTitle, pointCode, ssn);
    }

    public static AbsoluteTimeStamp getAbsoluteTimeStampImpl() {
        Calendar calendar = new GregorianCalendar();
        return toAbsoluteTimeStamp(calendar);
    }

    public static AbsoluteTimeStamp epochUtcTimeToAbsoluteTimeStampImpl(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return toAbsoluteTimeStamp(calendar);
    }

    private static AbsoluteTimeStamp toAbsoluteTimeStamp(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int mon = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int h = calendar.get(Calendar.HOUR);
        int m = calendar.get(Calendar.MINUTE);
        int s = calendar.get(Calendar.SECOND);
        int tz = calendar.get(Calendar.ZONE_OFFSET);
        return new AbsoluteTimeStampImpl(year - 2000, mon, day, h, m, s, tz / 1000 / 60 / 15);
    }

    public static Calendar toCalendar(AbsoluteTimeStamp absoluteTimeStamp) {
        Calendar calendarDate = Calendar.getInstance();
        calendarDate.set(Calendar.YEAR, absoluteTimeStamp.getYear());
        calendarDate.set(Calendar.MONTH, absoluteTimeStamp.getMonth() - 1);
        calendarDate.set(Calendar.DAY_OF_MONTH, absoluteTimeStamp.getDay());
        calendarDate.set(Calendar.HOUR_OF_DAY, absoluteTimeStamp.getHour());
        calendarDate.set(Calendar.MINUTE, absoluteTimeStamp.getMinute());
        calendarDate.set(Calendar.SECOND, absoluteTimeStamp.getSecond());
        calendarDate.set(Calendar.MILLISECOND, 0);
        return calendarDate;
    }

    public static boolean checkDataCodingSchemeSupport(int dcs) {
        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
        return (!dataCodingScheme.getIsCompressed()) && (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7 || dataCodingScheme.getCharacterSet() == CharacterSet.UCS2);
    }


    public static ISDNAddressString getMsisdn(MessageEvent message) {
        int addressNatureValue = message.getAddressNatureMsisdn() != null ? message.getAddressNatureMsisdn() : 0;
        int numberingPlanValue = message.getNumberingPlanMsisdn() != null ? message.getNumberingPlanMsisdn() : 0;

        var addressNature = CustomTypeOfNumber.fromSmsc((byte) addressNatureValue).getPrimitiveValue();
        var numberingPlan = CustomNumberingPlanIndicator.fromSmsc((byte) numberingPlanValue).getPrimitiveValue();

        log.debug("Creating msisdn with values AddressNature: {}, NumberingPlan: {}, MSISDN:{}",
                addressNature, numberingPlan, message.getMsisdn());

        return new ISDNAddressStringImpl(addressNature, numberingPlan, message.getMsisdn());
    }

    public static String getMapErrorCodeToString(MAPErrorMessage mapErrorMessage) {
        long eCode = Optional.ofNullable(mapErrorMessage).map(MAPErrorMessage::getErrorCode).orElse(-1L);
        return errorToStringMap.get(eCode);
    }

    //Listeners
    public static ChannelMessage createChannelMessage(String messageType) {
        String transactionId = System.currentTimeMillis() + "-" + System.nanoTime();
        ChannelMessage channelMessage = new ChannelMessage(transactionId, "Map");
        channelMessage.setParameter(Constants.MESSAGE_TYPE, messageType);
        return channelMessage;
    }

}
