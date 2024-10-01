package com.paicbd.module.utils;

import com.paicbd.module.ss7.layer.impl.MessageUtil;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.junit.jupiter.api.Test;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.indicator.NumberingPlan;
import org.restcomm.protocols.ss7.indicator.RoutingIndicator;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageSystemFailureImpl;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageUnknownSubscriberImpl;
import org.restcomm.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.restcomm.protocols.ss7.sccp.impl.parameter.BCDOddEncodingScheme;
import org.restcomm.protocols.ss7.sccp.impl.parameter.DefaultEncodingScheme;
import org.restcomm.protocols.ss7.sccp.impl.parameter.GlobalTitle0001Impl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.GlobalTitle0010Impl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.GlobalTitle0011Impl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.GlobalTitle0100Impl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.NoGlobalTitle;
import org.restcomm.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.restcomm.protocols.ss7.sccp.parameter.EncodingScheme;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;

import static com.paicbd.module.utils.Ss7Utils.AssociationType.CLIENT;
import static com.paicbd.module.utils.Ss7Utils.AssociationType.SERVER;
import static com.paicbd.module.utils.Ss7Utils.LayerType.M3UA;
import static com.paicbd.module.utils.Ss7Utils.LayerType.MAP;
import static com.paicbd.module.utils.Ss7Utils.LayerType.SCCP;
import static com.paicbd.module.utils.Ss7Utils.LayerType.SCTP;
import static com.paicbd.module.utils.Ss7Utils.LayerType.TCAP;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class Ss7UtilsTest {

    //For GlobalTitle Test
    String digits = "88888888";

    int pointCode = 100;
    int ssn = 8;

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        Constructor<Ss7Utils> constructor = Ss7Utils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }


    @Test
    void testGetGlobalTitle001() {
        String gtIndicator = "GT0001";
        var natureOfAddress = NatureOfAddress.valueOf(4);
        var globalTitle0001 = new GlobalTitle0001Impl(digits, natureOfAddress);
        var result = Ss7Utils.getGlobalTitle(gtIndicator, 0, null, NumberingPlan.ISDN_TELEPHONY, natureOfAddress, digits);
        assertEquals(globalTitle0001, result);
    }

    @Test
    void testGetGlobalTitle0010() {
        String gtIndicator = "GT0010";
        int translationType = 0;
        var globalTitle0010 = new GlobalTitle0010Impl(digits, translationType);
        var result = Ss7Utils.getGlobalTitle(gtIndicator, translationType, null, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.UNKNOWN, digits);
        assertEquals(globalTitle0010, result);
    }

    @Test
    void testGetGlobalTitle0011_encodingSchemaBCDOdd() {
        testGetGlobalTitle0011ByEncoding(1);
    }

    @Test
    void testGetGlobalTitle0011_encodingSchemaBCDEven() {
        testGetGlobalTitle0011ByEncoding(2);
    }

    @Test
    void testGetGlobalTitle0011_encodingSchemaDefault() {
        testGetGlobalTitle0011ByEncoding(3);
    }

    private void testGetGlobalTitle0011ByEncoding(int encodingSchemeType) {
        String gtIndicator = "GT0011";
        int translationType = 0;
        var numberingPlan = NumberingPlan.valueOf(1);
        EncodingScheme encodingScheme = switch (encodingSchemeType) {
            case 1 -> new BCDOddEncodingScheme();
            case 2 -> new BCDEvenEncodingScheme();
            default -> new DefaultEncodingScheme();
        };
        var globalTitle0011 = new GlobalTitle0011Impl(digits, translationType, encodingScheme, numberingPlan);
        var result = Ss7Utils.getGlobalTitle(gtIndicator, translationType, encodingScheme, numberingPlan, NatureOfAddress.UNKNOWN, digits);
        assertEquals(globalTitle0011, result);
    }

    @Test
    void testGetGlobalTitle0100_encodingSchemaBCDOdd() {
        testGetGlobalTitle0100ByEncoding(1);
    }

    @Test
    void testGetGlobalTitle0100_encodingSchemaBCDEven() {
        testGetGlobalTitle0100ByEncoding(2);
    }

    @Test
    void testGetGlobalTitle0100_encodingSchemaDefault() {
        testGetGlobalTitle0100ByEncoding(3);

    }

    private void testGetGlobalTitle0100ByEncoding(int encodingSchemeType) {
        String gtIndicator = "GT0100";
        int translationType = 0;
        var numberingPlan = NumberingPlan.valueOf(1);
        var natureOfAddress = NatureOfAddress.valueOf(4);
        EncodingScheme encodingScheme = switch (encodingSchemeType) {
            case 1 -> new BCDOddEncodingScheme();
            case 2 -> new BCDEvenEncodingScheme();
            default -> new DefaultEncodingScheme();
        };
        var globalTitle0100 = new GlobalTitle0100Impl(digits, translationType, encodingScheme, numberingPlan, natureOfAddress);
        var result = Ss7Utils.getGlobalTitle(gtIndicator, translationType, encodingScheme, numberingPlan, natureOfAddress, digits);
        assertEquals(globalTitle0100, result);
    }

    @Test
    void testGetGlobalTitleDefault() {
        String gtIndicator = "DEFAULT";
        var noGlobalTitle = new NoGlobalTitle(digits);
        var result = Ss7Utils.getGlobalTitle(gtIndicator, 0, null, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.UNKNOWN, digits);
        assertEquals(noGlobalTitle, result);
    }

    @Test
    void testConvertToSccpAddress_GlobalTitle001() {
        int natureOfAddress = 4;
        var globalTitle0001 = new GlobalTitle0001Impl(digits, NatureOfAddress.valueOf(natureOfAddress));
        testConvertToSccpAddressByGtIndicator(globalTitle0001);
    }

    @Test
    void testConvertToSccpAddress_GlobalTitle0010() {
        int translationType = 0;
        var globalTitle0010 = new GlobalTitle0010Impl(digits, translationType);
        testConvertToSccpAddressByGtIndicator(globalTitle0010);
    }

    @Test
    void testConvertToSccpAddress_GlobalTitle0011() {
        int translationType = 0;
        int numberingPlan = 1;
        var globalTitle0011 = new GlobalTitle0011Impl(digits, translationType, new BCDOddEncodingScheme(), NumberingPlan.valueOf(numberingPlan));
        testConvertToSccpAddressByGtIndicator(globalTitle0011);

    }

    @Test
    void testConvertToSccpAddress_GlobalTitle0100() {
        int translationType = 0;
        int numberingPlan = 1;
        int natureOfAddress = 4;
        var globalTitle0100 = new GlobalTitle0100Impl(
                digits, translationType, new BCDOddEncodingScheme(),
                NumberingPlan.valueOf(numberingPlan), NatureOfAddress.valueOf(natureOfAddress));
        testConvertToSccpAddressByGtIndicator(globalTitle0100);
    }

    @Test
    void testConvertToSccpAddress_GlobalTitleDefault() {
        var noGlobalTitle = new NoGlobalTitle(digits);
        testConvertToSccpAddressByGtIndicator(noGlobalTitle);
    }

    private void testConvertToSccpAddressByGtIndicator(GlobalTitle globalTitle) {
        var sccpAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, globalTitle, pointCode, ssn);
        var result = Ss7Utils.convertToSccpAddress(globalTitle, pointCode, ssn);
        assertEquals(sccpAddress, result);
    }

    @Test
    void testGetAbsoluteTimeStampImpl() {
        var resultTime = Ss7Utils.getAbsoluteTimeStampImpl();
        assertInstanceOf(AbsoluteTimeStampImpl.class, resultTime);
    }

    @Test
    void testEpochUtcTimeToAbsoluteTimeStampImpl() {
        //UTC Date: 2024-06-17
        long epochTime = 1721228701656L;
        var resultTime = Ss7Utils.epochUtcTimeToAbsoluteTimeStampImpl(epochTime);
        assertEquals(24, resultTime.getYear());
        assertEquals(6, resultTime.getMonth());
        assertEquals(17, resultTime.getDay());

    }

    @Test
    void testCalendar() {
        var absoluteTimeStamp = Ss7Utils.getAbsoluteTimeStampImpl();
        var resultTime = Ss7Utils.toCalendar(absoluteTimeStamp);
        assertInstanceOf(Calendar.class, resultTime);
        assertEquals(absoluteTimeStamp.getYear(), resultTime.get(Calendar.YEAR));
        assertEquals(absoluteTimeStamp.getMonth(), resultTime.get(Calendar.MONTH) + 1);
        assertEquals(absoluteTimeStamp.getDay(), resultTime.get(Calendar.DAY_OF_MONTH));
        assertEquals(absoluteTimeStamp.getHour(), resultTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(absoluteTimeStamp.getMinute(), resultTime.get(Calendar.MINUTE));
        assertEquals(absoluteTimeStamp.getSecond(), resultTime.get(Calendar.SECOND));
    }

    @Test
    void testCheckDataCodingSchemeSupport() {
        assertTrue(Ss7Utils.checkDataCodingSchemeSupport(0));//GSM7
        assertTrue(Ss7Utils.checkDataCodingSchemeSupport(1));//GSM7
        assertTrue(Ss7Utils.checkDataCodingSchemeSupport(2));//UCS2

        assertFalse(Ss7Utils.checkDataCodingSchemeSupport(15));
    }


    @Test
    void testGetMapErrorCodeToString() {
        assertEquals("System Failure", Ss7Utils.getMapErrorCodeToString(new MAPErrorMessageSystemFailureImpl()));
        assertEquals("Unknown Subscriber", Ss7Utils.getMapErrorCodeToString(new MAPErrorMessageUnknownSubscriberImpl()));
        assertEquals("System Failure", Ss7Utils.getMapErrorCodeToString(null));

    }

    @Test
    void testEnumValuesOfLayerType() {
        Ss7Utils.LayerType[] expectedValues = {
                SCTP, M3UA, SCCP, TCAP, MAP
        };
        assertArrayEquals(expectedValues, Ss7Utils.LayerType.values());
    }

    @Test
    void testEnumValuesOfAssociationType() {
        Ss7Utils.AssociationType[] expectedValues = {
                CLIENT, SERVER
        };
        assertArrayEquals(expectedValues, Ss7Utils.AssociationType.values());
    }

    @Test
    void testGetMessage() {
        var channelMessage = Ss7Utils.getMessage(Constants.DIALOG);
        assertNotNull(channelMessage.getOriginId());
        assertNotNull(channelMessage.getTransactionId());
        assertEquals(Constants.DIALOG, channelMessage.getParameter(Constants.MESSAGE_TYPE));
    }

    @Test
    void testGetMsisdnWithDefaultValues() {
        var message = MessageUtil.getMessageEvent();
        message.setAddressNatureMsisdn(null);
        message.setNumberingPlanMsisdn(null);
        var msisdn = Ss7Utils.getMsisdn(message);
        assertEquals(AddressNature.unknown, msisdn.getAddressNature());
        assertEquals(org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.unknown,
                msisdn.getNumberingPlan());
    }

    @Test
    void testGetMsisdn() {
        var message = MessageUtil.getMessageEvent();
        message.setAddressNatureMsisdn(1);
        message.setNumberingPlanMsisdn(1);
        var msisdn = Ss7Utils.getMsisdn(message);
        assertEquals(AddressNature.international_number, msisdn.getAddressNature());
        assertEquals(org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
                msisdn.getNumberingPlan());
    }

    @Test
    void testCustomNumberingPlanIndicator() {
        assertAll("FromSmsc",
                () -> assertEquals(CustomNumberingPlanIndicator.ISDN, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.ISDN.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.UNKNOWN, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.UNKNOWN.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.DATA, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.DATA.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.TELEX, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.TELEX.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.LAND_MOBILE, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.LAND_MOBILE.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.NATIONAL, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.NATIONAL.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.PRIVATE, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.PRIVATE.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.ERMES, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.ERMES.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.INTERNET, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.INTERNET.value())),
                () -> assertEquals(CustomNumberingPlanIndicator.WAP, CustomNumberingPlanIndicator.fromSmsc(NumberingPlanIndicator.WAP.value()))
        );

        assertAll("FromSmsTpdu",
                () -> assertEquals(CustomNumberingPlanIndicator.ISDN, CustomNumberingPlanIndicator.fromSmsTpdu(NumberingPlanIdentification.ISDNTelephoneNumberingPlan)),
                () -> assertEquals(CustomNumberingPlanIndicator.UNKNOWN, CustomNumberingPlanIndicator.fromSmsTpdu(NumberingPlanIdentification.Reserved))
        );

        assertAll("FromPrimitive",
                () -> assertEquals(CustomNumberingPlanIndicator.ISDN, CustomNumberingPlanIndicator.fromPrimitive(org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN)),
                () -> assertEquals(CustomNumberingPlanIndicator.UNKNOWN, CustomNumberingPlanIndicator.fromPrimitive(org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.reserved))
        );
    }

    @Test
    void testCustomTypeOfNumber() {
        assertAll("FromSmsc",
                () -> assertEquals(CustomTypeOfNumber.INTERNATIONAL, CustomTypeOfNumber.fromSmsc(TypeOfNumber.INTERNATIONAL.value())),
                () -> assertEquals(CustomTypeOfNumber.UNKNOWN, CustomTypeOfNumber.fromSmsc(TypeOfNumber.UNKNOWN.value())),
                () -> assertEquals(CustomTypeOfNumber.NATIONAL, CustomTypeOfNumber.fromSmsc(TypeOfNumber.NATIONAL.value())),
                () -> assertEquals(CustomTypeOfNumber.NETWORK_SPECIFIC, CustomTypeOfNumber.fromSmsc(TypeOfNumber.NETWORK_SPECIFIC.value())),
                () -> assertEquals(CustomTypeOfNumber.SUBSCRIBER_NUMBER, CustomTypeOfNumber.fromSmsc(TypeOfNumber.SUBSCRIBER_NUMBER.value())),
                () -> assertEquals(CustomTypeOfNumber.ALPHANUMERIC, CustomTypeOfNumber.fromSmsc(TypeOfNumber.ALPHANUMERIC.value())),
                () -> assertEquals(CustomTypeOfNumber.ABBREVIATED, CustomTypeOfNumber.fromSmsc(TypeOfNumber.ABBREVIATED.value()))
        );

        assertAll("FromSmsTpdu",
                () -> assertEquals(CustomTypeOfNumber.INTERNATIONAL, CustomTypeOfNumber.fromSmsTpdu(org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.InternationalNumber)),
                () -> assertEquals(CustomTypeOfNumber.UNKNOWN, CustomTypeOfNumber.fromSmsTpdu(org.restcomm.protocols.ss7.map.api.smstpdu.TypeOfNumber.Reserved))
        );

        assertAll("FromPrimitive",
                () -> assertEquals(CustomTypeOfNumber.INTERNATIONAL, CustomTypeOfNumber.fromPrimitive(AddressNature.international_number)),
                () -> assertEquals(CustomTypeOfNumber.UNKNOWN, CustomTypeOfNumber.fromPrimitive(AddressNature.reserved_for_extension))
        );
    }

}