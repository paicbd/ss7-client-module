package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.module.utils.Ss7Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class MapLayerTest {

    @Mock
    AppProperties appProperties;

    @Mock
    ExtendedResource extendedResource;

    String path;

    SctpLayer sctpLayer;

    M3uaLayer m3uaLayer;

    SccpLayer sccpLayer;

    TcapLayer tcapLayer;

    MapLayer mapLayer;


    @BeforeEach
    void setUp() {
        extendedResource = new ExtendedResource(appProperties);
        when(appProperties.getConfigPath()).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        extendedResource.deleteDirectory(new File(path));
    }

    @Test
    @DisplayName("Start Layer when gateway is correct then do it successfully")
    void startLayerWithMapSettingsThenDoItSuccessfully() {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());

        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, gateway, path, m3uaLayer);

        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, gateway, path, sccpLayer);

        mapLayer = (MapLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-MAP", Ss7Utils.LayerType.MAP, gateway, path, tcapLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());
        assertDoesNotThrow(() -> sccpLayer.start());
        assertDoesNotThrow(() -> tcapLayer.start());
        assertDoesNotThrow(() -> mapLayer.start());

        assertNotNull(mapLayer.getMapProvider());
        assertNotNull(mapLayer.getMapStack());
        assertNotNull(mapLayer.getName());

        assertTrue(mapLayer.getMapStack().getTCAPStack().isStarted());
        assertTrue(mapLayer.getMapStack().getTCAPStack().getSccpStack().isStarted());

        assertDoesNotThrow(() -> mapLayer.stop());
        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }



}