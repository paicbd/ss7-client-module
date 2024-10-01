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
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SctpLayerTest {

    @Mock
    AppProperties appProperties;

    @InjectMocks
    ExtendedResource extendedResource;

    String path;
    Gateway ss7Gateway = GatewayUtil.getGateway(2703, 2704);
    SctpLayer sctpLayer;


    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
    }

    @AfterEach
    void tearDown() throws IOException {
        extendedResource.deleteDirectory(new File(path));
    }

    @Test
    void testStartLayer() {
        assertNotNull(sctpLayer.getSctpManagement());
        assertInstanceOf(NettySctpManagementImpl.class, sctpLayer.getSctpManagement());

        assertNotNull(sctpLayer.getName());
        assertEquals(ss7Gateway.getName() + "-SCTP", sctpLayer.getName());

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testStartLayerWithDefaultValues() {
        ss7Gateway.getSettingsM3UA().getGeneral().setCcDelayThreshold1(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setCcDelayThreshold2(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setCcDelayThreshold3(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setCcDelayBackToNormalThreshold1(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setCcDelayBackToNormalThreshold2(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setCcDelayBackToNormalThreshold3(0);
        ss7Gateway.getSettingsM3UA().getGeneral().setThreadCount(1);
        testStartLayer();
    }

    @Test
    void testStartLayerAsServer() {
        var socketList = getSockets(1, "Server");
        ss7Gateway.getSettingsM3UA().getAssociations().setSockets(socketList);
        testStartLayer();
    }

    @Test
    void testStart_throwsExceptionOnloadAssociations() {
        var socketList = getSockets(1, "WrongType");
        ss7Gateway.getSettingsM3UA().getAssociations().setSockets(socketList);
        assertThrows(RTException.class, () -> sctpLayer.start());
    }

    @Test
    void testStart_throwsExceptionOnloadAssociations_socketNull() {
        var socketList = getSockets(11, "Client");
        ss7Gateway.getSettingsM3UA().getAssociations().setSockets(socketList);
        assertThrows(RTException.class, () -> sctpLayer.start());
    }

    private ArrayList<SettingsM3UA.Associations.Socket> getSockets(int id, String socketType) {
        SettingsM3UA.Associations.Socket socket = new SettingsM3UA.Associations.Socket();
        socket.setId(id);
        socket.setName("socket");
        socket.setState("STOPPED");
        socket.setEnabled(0);
        socket.setSocketType(socketType);
        socket.setTransportType("SCTP");
        socket.setHostAddress("127.0.0.1");
        socket.setHostPort(2910);
        socket.setMaxConcurrentConnections(10);
        socket.setSs7M3uaId(1);
        var socketList = new ArrayList<>(ss7Gateway.getSettingsM3UA().getAssociations().getSockets());
        socketList.set(0, socket);
        return socketList;
    }

}