package com.paicbd.module.utils;

import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.indicator.NumberingPlan;
import org.restcomm.protocols.ss7.indicator.RoutingIndicator;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
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
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle0001;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle0010;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle0011;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(MockitoExtension.class)
class Ss7UtilsTest {

    static final String DIGITS = "88888888";
    static final int ALLOWED_DELAY_FOR_MIN_OR_SEC = 10;

    enum TimeOfCalendar {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND
    }

    @ParameterizedTest
    @DisplayName("""
                getGlobalTitleParameters when digit, natureOfAddress, translationType, numberingPlan, encodingScheme, natureOfAddress
                 parameters provided then return the global title depending on the gtIndicator
            """)
    @MethodSource("getGlobalTitleParameters")
    void getGlobalTitleWhenGtIndicatorSentThenReturnResponse(String gtIndicator, NatureOfAddress natureOfAddress,
                        GlobalTitle expectedGlobalTitle, EncodingScheme expectedEncoding) {
        var result = Ss7Utils.getGlobalTitle(gtIndicator, 0, expectedEncoding,
                NumberingPlan.ISDN_TELEPHONY, natureOfAddress, DIGITS);

        assertEquals(expectedGlobalTitle, result);
    }

    @ParameterizedTest
    @MethodSource("getAbsoluteTimeStampParameters")
    @DisplayName("getAbsoluteTimeStampImpl when a calendar instance is created then return the date in the AbsoluteTimeStamp type")
    void getAbsoluteTimeStampImplWhenCalendarInstanceThenReturnAbsoluteTime(TimeOfCalendar value, int expectedDateValue) {
        AbsoluteTimeStamp result = Ss7Utils.getAbsoluteTimeStampImpl();
        int timeResult = getTimeOfCalendar(value, result);
        boolean isValidResult = dateValuesAreTheSame(value, timeResult, expectedDateValue);
        assertTrue(isValidResult);
    }

    @ParameterizedTest
    @MethodSource("epochUtcTimeToAbsoluteParameters")
    @DisplayName("""
            epochUtcTimeToAbsoluteTimeStampImpl when provide an epoch formated date via a Long number
            Then return the date in the AbsoluteTimeStamp type
            """)
    void epochUtcTimeToAbsoluteTimeStampImplWhenEpochThenReturnAbsoluteTime(long inputTime, TimeOfCalendar value, int expectedDateValue) {
        AbsoluteTimeStamp result = Ss7Utils.epochUtcTimeToAbsoluteTimeStampImpl(inputTime);
        int timeResult = getTimeOfCalendar(value, result);
        boolean isValidResult = dateValuesAreTheSame(value, timeResult, expectedDateValue);
        assertTrue(isValidResult);

    }

    @ParameterizedTest
    @MethodSource("getMsisdnParameters")
    @DisplayName("""
            getMsisdn when a message event is sent to the method then return the corresponding ISDN Address
            based on the addressNature and the numberingPlan found using the message event object
            """)
    void getMsisdnWhenMessageEventSentThenReturnIsdnAddress(Integer addressNature, AddressNature expectedAddressNature,
                   Integer numberingPlan, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan expectedNumberingPlan) {
        MessageEvent messageEvent = MessageEvent.builder()
                .messageId("1")
                .shortMessage("Test Message")
                .delReceipt("")
                .dataCoding(0)
                .esmClass(3)
                .sourceAddrTon(1)
                .numberingPlanMsisdn(numberingPlan)
                .sourceAddrNpi(1)
                .destAddrTon(1)
                .addressNatureMsisdn(addressNature)
                .destAddrNpi(1)
                .sourceAddr("1234")
                .destinationAddr("5678")
                .build();

        ISDNAddressString result = Ss7Utils.getMsisdn(messageEvent);
        assertEquals(expectedAddressNature, result.getAddressNature());
        assertEquals(expectedNumberingPlan, result.getNumberingPlan());
    }

    @ParameterizedTest
    @MethodSource("getMapErrorCodeParameters")
    @DisplayName("""
            getMapErrorCodeToString when te mapErrorMessage object is passed to the method then return the error code associated
            looking for the matching value in the errorToString map
            """)
    void getMapErrorCodeToStringWhenErrorMessageSentThenReturnTheCode(MAPErrorMessage errorMessage, String expectedErrorCode) {
        String result = Ss7Utils.getMapErrorCodeToString(errorMessage);
        assertEquals(expectedErrorCode, result);
    }

    @ParameterizedTest
    @MethodSource("createChannelParameters")
    @DisplayName("""
            createChannelMessage when the message type is provided
            then it is associated to a new created ChannelMessage object
            finally returns the modified channel message
            """)
    void createChannelMessageWhenMessageTypeThenReturnChannelMessage(String messageType, String expectedValue) {
        ChannelMessage result = Ss7Utils.createChannelMessage(messageType);
        assertEquals(messageType, result.getParameter(expectedValue));

    }

    @ParameterizedTest
    @MethodSource("checkDataCodingSchemeParameters")
    @DisplayName("""
            checkDataCodingSchemeSupport when passing a dcs value then it verifies if:
            the coding scheme is compressed, if the character set matches with GSM7 or UCS2
            and based on the validation then return true or false
            """)
    void checkDataCodingSchemeSupportWhenDcsSentThenReturnResult(int dcs, boolean expectedValue) {
        boolean result = Ss7Utils.checkDataCodingSchemeSupport(dcs);
        assertEquals(result, expectedValue);
    }

    @Test
    @DisplayName("""
            convertToSccpAddress when the globalTitle, the pointCode and the ssn is provided
            then return a new SccpAddress implementation with it's corresponding global routing indicator
            """)
    void convertToSccpAddressWhenParametersSentThenReturnSccpAddress() {

        int pointCode = 100;
        int ssn = 8;
        int natureOfAddress = 4;
        String digits = "1000";
        var noGlobalTitle = new NoGlobalTitle(digits);
        int translationType = 0;


        var sccpAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, noGlobalTitle, pointCode, ssn);
        var result = Ss7Utils.convertToSccpAddress(noGlobalTitle, pointCode, ssn);
        assertEquals(sccpAddress, result);

        var globalTitle0001 = new GlobalTitle0001Impl(digits, NatureOfAddress.valueOf(natureOfAddress));
        var sccpAddress2 = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, globalTitle0001, pointCode, ssn);
        var result2 = Ss7Utils.convertToSccpAddress(globalTitle0001, pointCode, ssn);
        assertEquals(sccpAddress2, result2);

        var globalTitle0100 = new GlobalTitle0100Impl(
                digits, translationType, new BCDOddEncodingScheme(),
                NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.valueOf(natureOfAddress));

        var sccpAddress3 = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, globalTitle0100, pointCode, ssn);
        var result3 = Ss7Utils.convertToSccpAddress(globalTitle0100, pointCode, ssn);
        assertEquals(sccpAddress3, result3);
    }

    @ParameterizedTest
    @MethodSource("toCalendarParameters")
    @DisplayName("""
            toCalendar when the absoluteTimeStamp formatted date is passes to the method
            then return a date based on a Calendar instance from years to millis
            """)
    void toCalendarWhenAbsoluteDateProvidedThenReturnCalendar(AbsoluteTimeStamp absoluteTimeStamp, TimeOfCalendar value, int expectedDateValue) {
        Calendar result = Ss7Utils.toCalendar(absoluteTimeStamp);
        int timeResult = getTimeOfCalendar(value, result);
        boolean isValidResult = dateValuesAreTheSame(value, timeResult, expectedDateValue);
        assertTrue(isValidResult);
    }

    static Stream<Arguments> getGlobalTitleParameters() {
        NatureOfAddress natureOfAddress04 = NatureOfAddress.valueOf(4);
        NumberingPlan numberingPlan01 = NumberingPlan.valueOf(1);
        BCDOddEncodingScheme bcodEncoding = new BCDOddEncodingScheme();
        BCDEvenEncodingScheme bcdeEncoding = new BCDEvenEncodingScheme();
        DefaultEncodingScheme defaultEncoding = new DefaultEncodingScheme();

        GlobalTitle0001 globalTitle0001 = new GlobalTitle0001Impl(DIGITS, natureOfAddress04);
        GlobalTitle0010 globalTitle0010 = new GlobalTitle0010Impl(DIGITS, 0);
        GlobalTitle0011 globalTitle0011Encoding1 = new GlobalTitle0011Impl(DIGITS, 0, bcodEncoding, numberingPlan01);
        GlobalTitle0011 globalTitle0011Encoding2 = new GlobalTitle0011Impl(DIGITS, 0, bcdeEncoding, numberingPlan01);
        GlobalTitle0011 globalTitle0011Encoding3 = new GlobalTitle0011Impl(DIGITS, 0, defaultEncoding, numberingPlan01);
        GlobalTitle0100Impl globalTitle0100Encoding1 = new GlobalTitle0100Impl(DIGITS, 0, bcodEncoding, numberingPlan01, NatureOfAddress.INTERNATIONAL);
        GlobalTitle0100Impl globalTitle0100Encoding2 = new GlobalTitle0100Impl(DIGITS, 0, bcdeEncoding, numberingPlan01, NatureOfAddress.INTERNATIONAL);
        GlobalTitle0100Impl globalTitle0100Encoding3 = new GlobalTitle0100Impl(DIGITS, 0, defaultEncoding, numberingPlan01, NatureOfAddress.INTERNATIONAL);
        NoGlobalTitle noGlobalTitle = new NoGlobalTitle(DIGITS);

        return Stream.of(
                Arguments.of("GT0001", natureOfAddress04, globalTitle0001, null),
                Arguments.of("GT0010", NatureOfAddress.UNKNOWN, globalTitle0010, null),
                Arguments.of("GT0011", NatureOfAddress.UNKNOWN, globalTitle0011Encoding1, bcodEncoding),
                Arguments.of("GT0011", NatureOfAddress.UNKNOWN, globalTitle0011Encoding2, bcdeEncoding),
                Arguments.of("GT0011", NatureOfAddress.UNKNOWN, globalTitle0011Encoding3, defaultEncoding),
                Arguments.of("GT0100", NatureOfAddress.INTERNATIONAL, globalTitle0100Encoding1, bcodEncoding),
                Arguments.of("GT0100", NatureOfAddress.INTERNATIONAL, globalTitle0100Encoding2, bcdeEncoding),
                Arguments.of("GT0100", NatureOfAddress.INTERNATIONAL, globalTitle0100Encoding3, defaultEncoding),
                Arguments.of("DEFAULT", NatureOfAddress.UNKNOWN, noGlobalTitle, defaultEncoding)
        );
    }

    static Stream<Arguments> getAbsoluteTimeStampParameters() {
        return Stream.of(
                Arguments.of(TimeOfCalendar.YEAR, (GregorianCalendar.getInstance().get(Calendar.YEAR)) - 2000),
                Arguments.of(TimeOfCalendar.MONTH, (GregorianCalendar.getInstance().get(Calendar.MONTH)) + 1),
                Arguments.of(TimeOfCalendar.DAY, (GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH))),
                Arguments.of(TimeOfCalendar.HOUR, (GregorianCalendar.getInstance().get(Calendar.HOUR))),
                Arguments.of(TimeOfCalendar.MINUTE, (GregorianCalendar.getInstance().get(Calendar.MINUTE))),
                Arguments.of(TimeOfCalendar.SECOND, (GregorianCalendar.getInstance().get(Calendar.SECOND)))
        );
    }

    static Stream<Arguments> epochUtcTimeToAbsoluteParameters() {
        return Stream.of(
                Arguments.of(1622845633000L, TimeOfCalendar.YEAR, 21),
                Arguments.of(1541370433000L, TimeOfCalendar.MONTH, 11)
        );
    }

    static Stream<Arguments> getMsisdnParameters() {
        return Stream.of(
                Arguments.of(null, AddressNature.unknown, null, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.unknown),
                Arguments.of(1, AddressNature.international_number, 1, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN),
                Arguments.of(2, AddressNature.national_significant_number, 1, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN),
                Arguments.of(3, AddressNature.network_specific_number, 1, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN),
                Arguments.of(6, AddressNature.abbreviated_number, 1, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.ISDN),
                Arguments.of(1, AddressNature.international_number, 6, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.land_mobile),
                Arguments.of(1, AddressNature.international_number, 8, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.national),
                Arguments.of(1, AddressNature.international_number, 9, org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan.private_plan)
        );
    }

    static Stream<Arguments> getMapErrorCodeParameters() {

        return Stream.of(
                Arguments.of(new MAPErrorMessageSystemFailureImpl(), "System Failure"),
                Arguments.of(new MAPErrorMessageUnknownSubscriberImpl(), "Unknown Subscriber"),
                Arguments.of(null, "System Failure")
        );
    }

    static Stream<Arguments> createChannelParameters() {
        return Stream.of(
                Arguments.of(Constants.DIALOG, "messageType"),
                Arguments.of(null, null)
        );
    }

    static Stream<Arguments> checkDataCodingSchemeParameters() {
        return Stream.of(
                Arguments.of(0, Boolean.TRUE),
                Arguments.of(1, Boolean.TRUE),
                Arguments.of(2, Boolean.TRUE),
                Arguments.of('a', Boolean.FALSE),
                Arguments.of('c', Boolean.FALSE)
        );
    }

    static Stream<Arguments> toCalendarParameters() {
        AbsoluteTimeStamp absoluteTimeStamp = new AbsoluteTimeStampImpl(2024, 11, 2, 11, 25, 24, 5);
        return Stream.of(
                Arguments.of(absoluteTimeStamp, TimeOfCalendar.YEAR, absoluteTimeStamp.getYear()),
                Arguments.of(absoluteTimeStamp, TimeOfCalendar.MONTH, absoluteTimeStamp.getMonth()),
                Arguments.of(absoluteTimeStamp, TimeOfCalendar.DAY, absoluteTimeStamp.getDay())
        );
    }

    private int getTimeOfCalendar(TimeOfCalendar time, AbsoluteTimeStamp result) {
        return switch (time) {
            case TimeOfCalendar.YEAR -> result.getYear();
            case TimeOfCalendar.MONTH -> result.getMonth();
            case TimeOfCalendar.DAY -> result.getDay();
            case TimeOfCalendar.HOUR -> result.getHour();
            case TimeOfCalendar.MINUTE -> result.getMinute();
            case TimeOfCalendar.SECOND -> result.getSecond();
        };
    }

    private int getTimeOfCalendar(TimeOfCalendar time, Calendar result) {
        return switch (time) {
            case TimeOfCalendar.YEAR -> result.get(Calendar.YEAR);
            case TimeOfCalendar.MONTH -> result.get(Calendar.MONTH) + 1;
            case TimeOfCalendar.DAY -> result.get(Calendar.DAY_OF_MONTH);
            case TimeOfCalendar.HOUR -> result.get(Calendar.HOUR_OF_DAY);
            case TimeOfCalendar.MINUTE -> result.get(Calendar.MINUTE);
            case TimeOfCalendar.SECOND -> result.get(Calendar.SECOND);
        };
    }

    private boolean dateValuesAreTheSame(TimeOfCalendar timeOfCalendar, int timeResult, int expectedDateValue) {
        boolean evaluateDelay = Arrays.asList(TimeOfCalendar.MINUTE, TimeOfCalendar.SECOND).contains(timeOfCalendar);
        return evaluateDelay ?
                (Math.abs(timeResult - expectedDateValue) < ALLOWED_DELAY_FOR_MIN_OR_SEC) :
                (timeResult == expectedDateValue);
    }
}
