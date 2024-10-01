package com.paicbd.module.ss7.layer.impl.network.layers.listeners;

import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import org.restcomm.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.restcomm.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.restcomm.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.restcomm.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.restcomm.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.dialog.MAPUserAbortChoiceImpl;
import org.restcomm.protocols.ss7.map.primitives.AddressStringImpl;
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextName;
import org.restcomm.protocols.ss7.tcap.asn.ApplicationContextNameImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class MapDialogListenerTest {

    @Mock
    MAPDialog mapDialog;

    @Mock
    MAPExtensionContainer mapExtensionContainer;

    @Mock
    IChannelHandler channelHandler;

    MapDialogListener mapDialogListener;



    long invokeId = 1L;

    @BeforeEach
    void setUp() {
        mapDialogListener = new MapDialogListener(channelHandler);
    }

    @Test
    void testOnDialogDelimiter() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogDelimiter(mapDialog));
    }

    @Test
    void testOnDialogRequest() {
        AddressString addressString = new AddressStringImpl(AddressNature.unknown, NumberingPlan.unknown, "11111");
        AddressString addressString1 = new AddressStringImpl(AddressNature.unknown, NumberingPlan.unknown, "22222");
        assertDoesNotThrow(() -> mapDialogListener.onDialogRequest(mapDialog, addressString, addressString1, mapExtensionContainer));
    }

    @Test
    void testOnDialogRequestEricsson() {
        AddressString addressString = new AddressStringImpl(AddressNature.unknown, NumberingPlan.unknown, "11111");
        AddressString addressString1 = new AddressStringImpl(AddressNature.unknown, NumberingPlan.unknown, "22222");
        AddressString addressString2 = new AddressStringImpl(AddressNature.unknown, NumberingPlan.unknown, "33333");
        AddressString addressString3 = new AddressStringImpl(AddressNature.unknown, NumberingPlan.unknown, "44444");
        assertDoesNotThrow(() -> mapDialogListener.onDialogRequestEricsson(
                mapDialog, addressString, addressString1, addressString2, addressString3));
    }

    @Test
    void testOnDialogAccept() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogAccept(mapDialog, mapExtensionContainer));
    }

    @Test
    void testOnDialogReject() {
        ApplicationContextName applicationContextName = new ApplicationContextNameImpl();
        assertDoesNotThrow(() -> mapDialogListener.onDialogReject(
                mapDialog, MAPRefuseReason.NoReasonGiven, applicationContextName, mapExtensionContainer));
    }

    @Test
    void testOnDialogUserAbort() {
        MAPUserAbortChoice mapUserAbortChoice = new MAPUserAbortChoiceImpl();
        assertDoesNotThrow(() -> mapDialogListener.onDialogUserAbort(mapDialog, mapUserAbortChoice, mapExtensionContainer));
    }

    @Test
    void testOnDialogProviderAbort() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogProviderAbort(
                mapDialog, MAPAbortProviderReason.ProviderMalfunction, MAPAbortSource.MAPProblem, mapExtensionContainer));
    }

    @Test
    void testOnDialogClose() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogClose(mapDialog));
    }

    @Test
    void testOnDialogNotice() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogNotice(
                mapDialog, MAPNoticeProblemDiagnostic.MessageCannotBeDeliveredToThePeer));
    }

    @Test
    void testOnDialogRelease() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogRelease(mapDialog));
    }

    @Test
    void testOnDialogTimeout() {
        assertDoesNotThrow(() -> mapDialogListener.onDialogTimeout(mapDialog));
    }
}