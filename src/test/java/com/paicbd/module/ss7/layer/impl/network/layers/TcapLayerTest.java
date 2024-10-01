package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.exception.RTException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.tcap.api.TCAPProvider;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TcapLayerTest {

    @Mock
    AppProperties appProperties;

    @InjectMocks
    ExtendedResource extendedResource;

    Gateway ss7Gateway = GatewayUtil.getGateway(2711, 2712);
    String path;
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;
    SccpLayer sccpLayer;
    TcapLayer tcapLayer;


    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
    }

    @AfterEach
    void tearDown() throws IOException {
        extendedResource.deleteDirectory(new File(path));
    }

    @Test
    void testStartLayer() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();

        assertNotNull(tcapLayer.getTcapProvider());
        assertInstanceOf(TCAPProvider.class, tcapLayer.getTcapProvider());

        assertNotNull(tcapLayer.getName());
        assertEquals(ss7Gateway.getName() + "-TCAP", tcapLayer.getName());

        assertDoesNotThrow(() -> tcapLayer.start());

        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }


    @Test
    void testStartLayer_withExtraSsn() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();
        var ssnList =  "8,6,124";
        ss7Gateway.getSettingsTCAP().setSsnList(ssnList);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
        assertDoesNotThrow(() -> tcapLayer.start());
        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testStartLayer_dialogTimeoutNegative() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();
        ss7Gateway.getSettingsTCAP().setDialogIdleTimeout(-1);
        assertThrows(RTException.class, () -> tcapLayer.start());
    }




}