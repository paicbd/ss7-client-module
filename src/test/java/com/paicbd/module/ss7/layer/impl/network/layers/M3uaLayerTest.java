package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsM3UA;
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
import org.restcomm.protocols.ss7.m3ua.impl.M3UAManagementImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class M3uaLayerTest {

    @Mock
    AppProperties appProperties;

    @InjectMocks
    ExtendedResource extendedResource;

    Gateway ss7Gateway = GatewayUtil.getGateway(2705, 2706);
    String path;
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;



    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
    }

    @AfterEach
    void tearDown() throws IOException {
        extendedResource.deleteDirectory(new File(path));
    }


    @Test
    void testStartLayer() {
        sctpLayer.start();
        assertNotNull(m3uaLayer.getMtp3UserPart());
        assertInstanceOf(M3UAManagementImpl.class, m3uaLayer.getMtp3UserPart());

        assertNotNull(m3uaLayer.getName());
        assertEquals(ss7Gateway.getName() + "-M3UA", m3uaLayer.getName());
        assertDoesNotThrow(() -> m3uaLayer.start());

        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testStartLayerWithDefaultValues() {
        ss7Gateway.getSettingsM3UA().getGeneral().setThreadCount(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setMaxSequenceNumber(null);
        ss7Gateway.getSettingsM3UA().getGeneral().setMaxForRoute(null);
        ss7Gateway.getSettingsM3UA().getGeneral().setRoutingLabelFormat(null);
        ss7Gateway.getSettingsM3UA().getGeneral().setRoutingKeyManagementEnabled(null);
        ss7Gateway.getSettingsM3UA().getGeneral().setUseLowestBitForLink(null);
        testStartLayer();
    }

    @Test
    void testStartLayerWithoutDefaultValues() {
        ss7Gateway.getSettingsM3UA().getGeneral().setThreadCount(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setMaxSequenceNumber(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setMaxForRoute(0);
        testStartLayer();
    }


    @Test
    void testStartNotPreviousLayerStarted() {
        assertThrows(RTException.class, () -> m3uaLayer.start());
    }

    @Test
    void testStartAsWithTheSameName() {
        SettingsM3UA.ApplicationServer appServer = new SettingsM3UA.ApplicationServer();
        appServer.setId(1);
        appServer.setName("as");
        appServer.setState("STARTED");
        appServer.setFunctionality("IPSP-CLIENT");
        appServer.setExchange("SE");
        appServer.setRoutingContext(101);
        appServer.setNetworkAppearance(102);
        appServer.setTrafficModeId(2);
        appServer.setMinimumAspForLoadshare(0);
        appServer.setAspFactories(List.of(1));
        var listOfApplicationServers = new ArrayList<>(ss7Gateway.getSettingsM3UA().getApplicationServers());
        listOfApplicationServers.add(appServer);
        ss7Gateway.getSettingsM3UA().setApplicationServers(listOfApplicationServers);
        sctpLayer.start();
        assertThrows(RTException.class, () -> m3uaLayer.start());
    }


    @Test
    void testStartWithWrongAS() {
        var listOfRoutes = new ArrayList<>(ss7Gateway.getSettingsM3UA().getRoutes());
        listOfRoutes.getFirst().setAppServers(List.of(2));
        ss7Gateway.getSettingsM3UA().setRoutes(listOfRoutes);
        sctpLayer.start();
        assertThrows(RTException.class, () -> m3uaLayer.start());
    }

    @Test
    void testStartWithAssociationNull() {
        var listOfApplicationServers = new ArrayList<>(ss7Gateway.getSettingsM3UA().getApplicationServers());
        listOfApplicationServers.getFirst().setAspFactories(List.of(2));
        ss7Gateway.getSettingsM3UA().setApplicationServers(listOfApplicationServers);
        sctpLayer.start();
        assertThrows(RTException.class, () -> m3uaLayer.start());
    }

}