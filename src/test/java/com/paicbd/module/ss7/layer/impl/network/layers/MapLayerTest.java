package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.ss7.layer.impl.channel.MapChannel;
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
import org.restcomm.protocols.ss7.map.MAPProviderImpl;
import org.restcomm.protocols.ss7.map.MAPStackImpl;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapLayerTest {

    @Mock
    AppProperties appProperties;

    @Mock
    MapChannel mapChannel;

    @InjectMocks
    ExtendedResource extendedResource;

    Gateway ss7Gateway = GatewayUtil.getGateway(2709, 2710);
    String path;
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;
    SccpLayer sccpLayer;
    TcapLayer tcapLayer;
    MapLayer mapLayer;




    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
        mapLayer = (MapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-MAP", Ss7Utils.LayerType.MAP, ss7Gateway, path, tcapLayer);
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
        tcapLayer.start();

        assertNotNull(mapLayer.getMapProvider());
        assertInstanceOf(MAPProviderImpl.class, mapLayer.getMapProvider());

        assertNotNull(mapLayer.getMapStack());
        assertInstanceOf(MAPStackImpl.class, mapLayer.getMapStack());

        assertNotNull(mapLayer.getName());
        assertEquals(ss7Gateway.getName() + "-MAP", mapLayer.getName());

        assertDoesNotThrow(() -> mapLayer.start());

        assertDoesNotThrow(() -> mapLayer.stop());
        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testStartLayer_withError() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();
        tcapLayer.start();
        mapLayer.start();

        assertThrows(RTException.class, () -> mapLayer.start());

        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testStop() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();
        tcapLayer.start();

        assertDoesNotThrow(() -> mapLayer.start());

        assertDoesNotThrow(() -> mapLayer.stop());
        assertDoesNotThrow(() -> tcapLayer.stop());
        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testSetChannelHandler() {
        assertDoesNotThrow(() -> mapLayer.setChannelHandler(mapChannel));
    }

}