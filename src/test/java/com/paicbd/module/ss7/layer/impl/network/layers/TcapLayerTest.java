package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsTCAP;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.module.utils.Ss7Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.tcap.api.TCAPStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TcapLayerTest {

    @Mock
    AppProperties appProperties;

    @Mock
    ExtendedResource extendedResource;

    String path;

    SctpLayer sctpLayer;

    M3uaLayer m3uaLayer;

    SccpLayer sccpLayer;

    TcapLayer tcapLayer;


    @BeforeEach
    void setUp() {
        extendedResource = new ExtendedResource(appProperties);
        when(appProperties.getConfigPath()).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        extendedResource.deleteDirectory(new File(path));
    }

    @ParameterizedTest
    @MethodSource("tcapSettingsToTest")
    @DisplayName("Start Layer when gateway is correct then do it successfully")
    void startLayerWithDifferentTcapSettingsThenDoItSuccessfully(SettingsTCAP settingsTCAP) {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        gateway.setSettingsTCAP(settingsTCAP);

        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, gateway, path, m3uaLayer);

        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, gateway, path, sccpLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());
        assertDoesNotThrow(() -> sccpLayer.start());
        assertDoesNotThrow(() -> tcapLayer.start());

        TCAPStack tcapStack = tcapLayer.getTcapProvider().getStack();
        assertTrue(tcapStack.isStarted());
        assertNotNull(tcapStack.getPersistDir());
        assertEquals(path, tcapStack.getPersistDir());

        this.checkTcapSettings(tcapStack, settingsTCAP);

        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    private void checkTcapSettings(TCAPStack tcapStack, SettingsTCAP settingsTCAP) {
        var ssnList = new ArrayList<>(Arrays.stream(settingsTCAP.getSsnList().split(","))
                .map(Integer::parseInt)
                .toList());

        int ssn = ssnList.removeFirst();

        if (!ssnList.isEmpty()) {
            assertTrue(tcapStack.getExtraSsns().containsAll(ssnList));
        }
        assertEquals(ssn, tcapStack.getSubSystemNumber());
        assertEquals(settingsTCAP.isPreviewMode(), tcapStack.getPreviewMode());
        assertEquals(settingsTCAP.getDialogIdRangeStart(), tcapStack.getDialogIdRangeStart());
        assertEquals(settingsTCAP.getDialogIdRangeEnd(), tcapStack.getDialogIdRangeEnd());
        assertEquals(settingsTCAP.getDialogIdleTimeout(), tcapStack.getDialogIdleTimeout());
        assertEquals(settingsTCAP.isDoNotSendProtocolVersion(), tcapStack.getDoNotSendProtocolVersion());
        assertEquals(settingsTCAP.getInvokeTimeout(), tcapStack.getInvokeTimeout());
        assertEquals(settingsTCAP.getMaxDialogs(), tcapStack.getMaxDialogs());
        assertEquals(settingsTCAP.isSwapTcapIdEnabled(), tcapStack.getSwapTcapIdBytes());
        assertEquals(settingsTCAP.getSlsRangeId(), tcapStack.getSlsRange());
        assertEquals(settingsTCAP.getExrDelayThr1(), tcapStack.getCongControl_ExecutorDelayThreshold_1());
        assertEquals(settingsTCAP.getExrDelayThr2(), tcapStack.getCongControl_ExecutorDelayThreshold_2());
        assertEquals(settingsTCAP.getExrDelayThr3(), tcapStack.getCongControl_ExecutorDelayThreshold_3());
        assertEquals(settingsTCAP.getExrBackToNormalDelayThr1(), tcapStack.getCongControl_ExecutorBackToNormalDelayThreshold_1());
        assertEquals(settingsTCAP.getExrBackToNormalDelayThr2(), tcapStack.getCongControl_ExecutorBackToNormalDelayThreshold_2());
        assertEquals(settingsTCAP.getExrBackToNormalDelayThr3(), tcapStack.getCongControl_ExecutorBackToNormalDelayThreshold_3());
        assertEquals(settingsTCAP.getMemoryMonitorThr1(), tcapStack.getCongControl_MemoryThreshold_1());
        assertEquals(settingsTCAP.getMemoryMonitorThr2(), tcapStack.getCongControl_MemoryThreshold_2());
        assertEquals(settingsTCAP.getMemoryMonitorThr3(), tcapStack.getCongControl_MemoryThreshold_3());
        assertEquals(settingsTCAP.getMemBackToNormalDelayThr1(), tcapStack.getCongControl_BackToNormalMemoryThreshold_1());
        assertEquals(settingsTCAP.getMemBackToNormalDelayThr2(), tcapStack.getCongControl_BackToNormalMemoryThreshold_2());
        assertEquals(settingsTCAP.getMemBackToNormalDelayThr3(), tcapStack.getCongControl_BackToNormalMemoryThreshold_3());
        assertEquals(settingsTCAP.isBlockingIncomingTcapMessages(), tcapStack.isCongControl_blockingIncomingTcapMessages());
    }

    static Stream<SettingsTCAP> tcapSettingsToTest() {
        return Stream.of(
                SettingsTCAP.builder()
                        .id(1)
                        .networkId(4)
                        .ssnList("8")
                        .previewMode(false)
                        .dialogIdleTimeout(100000)
                        .invokeTimeout(25000)
                        .dialogIdRangeStart(1)
                        .dialogIdRangeEnd(2147483647)
                        .maxDialogs(5000)
                        .doNotSendProtocolVersion(false)
                        .swapTcapIdEnabled(true)
                        .slsRangeId("All")
                        .exrDelayThr1(1.0)
                        .exrDelayThr2(6.0)
                        .exrDelayThr3(12.0)
                        .memoryMonitorThr1(77)
                        .memoryMonitorThr2(87)
                        .memoryMonitorThr3(97)
                        .memBackToNormalDelayThr1(72)
                        .memBackToNormalDelayThr2(82)
                        .memBackToNormalDelayThr3(92)
                        .blockingIncomingTcapMessages(false)
                        .build(),
                //TCAP with extra ssn
                SettingsTCAP.builder()
                        .id(1)
                        .networkId(4)
                        .ssnList("8,6,124")
                        .previewMode(false)
                        .dialogIdleTimeout(100000)
                        .invokeTimeout(25000)
                        .dialogIdRangeStart(1)
                        .dialogIdRangeEnd(2147483647)
                        .maxDialogs(5000)
                        .doNotSendProtocolVersion(false)
                        .swapTcapIdEnabled(true)
                        .slsRangeId("All")
                        .exrDelayThr1(1.0)
                        .exrDelayThr2(6.0)
                        .exrDelayThr3(12.0)
                        .memoryMonitorThr1(77)
                        .memoryMonitorThr2(87)
                        .memoryMonitorThr3(97)
                        .memBackToNormalDelayThr1(72)
                        .memBackToNormalDelayThr2(82)
                        .memBackToNormalDelayThr3(92)
                        .blockingIncomingTcapMessages(false)
                        .build()
        );
    }
}