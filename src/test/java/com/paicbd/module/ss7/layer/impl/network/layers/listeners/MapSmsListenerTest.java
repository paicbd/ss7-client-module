package com.paicbd.module.ss7.layer.impl.network.layers.listeners;

import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.MAPMessage;
import org.restcomm.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.restcomm.protocols.ss7.map.service.sms.AlertServiceCentreRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.AlertServiceCentreResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.ForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.ForwardShortMessageResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.InformServiceCentreRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.MoForwardShortMessageResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.MtForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.MtForwardShortMessageResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.NoteSubscriberPresentRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.ReadyForSMRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.ReadyForSMResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.ReportSMDeliveryStatusRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.ReportSMDeliveryStatusResponseImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.restcomm.protocols.ss7.tcap.asn.comp.Problem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@ExtendWith(MockitoExtension.class)
class MapSmsListenerTest {

    @Mock
    MAPDialog mapDialog;

    @Mock
    MAPMessage mapMessage;

    @Mock
    MAPErrorMessage mapErrorMessage;

    @Mock
    Problem problem;

    @Mock
    IChannelHandler channelHandler;

    MapSmsListener mapSmsListener;

    long invokeId = 1L;

    @BeforeEach
    void setUp() {
        mapSmsListener = new MapSmsListener(channelHandler);
    }

    @Test
    void testOnErrorComponent() {
        assertDoesNotThrow(() -> mapSmsListener.onErrorComponent(mapDialog, invokeId, mapErrorMessage));
    }


    @Test
    void testOnRejectComponent() {
        assertDoesNotThrow(() -> mapSmsListener.onRejectComponent(mapDialog, invokeId, problem, true));
        assertDoesNotThrow(() -> mapSmsListener.onRejectComponent(mapDialog, invokeId, problem, false));
    }

    @Test
    void testOnInvokeTimeout() {
        assertDoesNotThrow(() -> mapSmsListener.onInvokeTimeout(mapDialog, invokeId));
    }

    @Test
    void testOnMAPMessage() {
        assertDoesNotThrow(() -> mapSmsListener.onMAPMessage(mapMessage));
    }

    @Test
    void testOnForwardShortMessageRequest() {
        var forwardShortMessageRequest = new ForwardShortMessageRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onForwardShortMessageRequest(forwardShortMessageRequest));
    }

    @Test
    void testOnForwardShortMessageResponse() {
        var forwardShortMessageResponse = new ForwardShortMessageResponseImpl();
        assertDoesNotThrow(() -> mapSmsListener.onForwardShortMessageResponse(forwardShortMessageResponse));
    }

    @Test
    void testOnMoForwardShortMessageRequest() {
        var moForwardShortMessageRequest = new MoForwardShortMessageRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onMoForwardShortMessageRequest(moForwardShortMessageRequest));
    }

    @Test
    void testOnMoForwardShortMessageResponse() {
        var moForwardShortMessageResponse = new MoForwardShortMessageResponseImpl();
        assertDoesNotThrow(() -> mapSmsListener.onMoForwardShortMessageResponse(moForwardShortMessageResponse));
    }

    @Test
    void testOnMtForwardShortMessageRequest() {
        var mtForwardShortMessageRequest = new MtForwardShortMessageRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onMtForwardShortMessageRequest(mtForwardShortMessageRequest));
    }


    @Test
    void testOnMtForwardShortMessageResponse() {
        var mtForwardShortMessageResponse = new MtForwardShortMessageResponseImpl();
        assertDoesNotThrow(() -> mapSmsListener.onMtForwardShortMessageResponse(mtForwardShortMessageResponse));
    }

    @Test
    void testOnSendRoutingInfoForSMRequest() {
        var sendRoutingInfoForSMRequest = new SendRoutingInfoForSMRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onSendRoutingInfoForSMRequest(sendRoutingInfoForSMRequest));
    }

    @Test
    void testOnSendRoutingInfoForSMResponse() {
        var sendRoutingInfoForSMResponse = new SendRoutingInfoForSMResponseImpl();
        assertDoesNotThrow(() -> mapSmsListener.onSendRoutingInfoForSMResponse(sendRoutingInfoForSMResponse));
    }

    @Test
    void testOnReportSMDeliveryStatusRequest() {
        var reportSMDeliveryStatusRequest = new ReportSMDeliveryStatusRequestImpl(3L);
        assertDoesNotThrow(() -> mapSmsListener.onReportSMDeliveryStatusRequest(reportSMDeliveryStatusRequest));
    }

    @Test
    void testOnReportSMDeliveryStatusResponse() {
        var reportSMDeliveryStatusResponse = new ReportSMDeliveryStatusResponseImpl(3L);
        assertDoesNotThrow(() -> mapSmsListener.onReportSMDeliveryStatusResponse(reportSMDeliveryStatusResponse));
    }

    @Test
    void testOnInformServiceCentreRequest() {
        var informServiceCentreRequest = new InformServiceCentreRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onInformServiceCentreRequest(informServiceCentreRequest));
    }

    @Test
    void testOnAlertServiceCentreRequest() {
        var alertServiceCentreRequest = new AlertServiceCentreRequestImpl(2);
        assertDoesNotThrow(() -> mapSmsListener.onAlertServiceCentreRequest(alertServiceCentreRequest));
    }

    @Test
    void testOnAlertServiceCentreResponse() {
        var alertServiceCentreResponse = new AlertServiceCentreResponseImpl();
        assertDoesNotThrow(() -> mapSmsListener.onAlertServiceCentreResponse(alertServiceCentreResponse));
    }

    @Test
    void testOnReadyForSMRequest() {
        var readyForSMRequest = new ReadyForSMRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onReadyForSMRequest(readyForSMRequest));
    }

    @Test
    void testOnReadyForSMResponse() {
        var readyForSMResponse = new ReadyForSMResponseImpl();
        assertDoesNotThrow(() -> mapSmsListener.onReadyForSMResponse(readyForSMResponse));
    }

    @Test
    void testOnNoteSubscriberPresentRequest() {
        var noteSubscriberPresentRequest = new NoteSubscriberPresentRequestImpl();
        assertDoesNotThrow(() -> mapSmsListener.onNoteSubscriberPresentRequest(noteSubscriberPresentRequest));
    }
}