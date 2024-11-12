package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsM3UA;
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
import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.m3ua.As;
import org.restcomm.protocols.ss7.m3ua.AspFactory;
import org.restcomm.protocols.ss7.m3ua.impl.M3UAManagementImpl;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SctpM3uaLayerTest {

    @Mock
    AppProperties appProperties;

    @Mock
    ExtendedResource extendedResource;

    String path;

    SctpLayer sctpLayer;

    M3uaLayer m3uaLayer;


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
    @DisplayName("startLayer when gateway is client then do it successfully")
    void startLayerWhenGatewayIsClientThenDoItSuccessfully() {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        SettingsM3UA.General m3uaGeneral = SettingsM3UA.General.builder()
                .id(1)
                .networkId(4)
                .connectDelay(5000)
                .maxSequenceNumber(256)
                .maxForRoute(2)
                .threadCount(1)
                .routingLabelFormat("ITU")
                .heartBeatTime(10000)
                .routingKeyManagementEnabled(false)
                .useLowestBitForLink(false)
                .ccDelayThreshold1(2.6)
                .ccDelayThreshold2(2.5)
                .ccDelayThreshold3(3.0)
                .ccDelayBackToNormalThreshold1(3.4)
                .ccDelayBackToNormalThreshold2(4.5)
                .ccDelayBackToNormalThreshold3(5.0)
                .build();

        SettingsM3UA.Associations.Socket socket = SettingsM3UA.Associations.Socket.builder()
                .id(1)
                .name("socket")
                .state("STARTED")
                .enabled(1)
                .socketType(Ss7Utils.AssociationType.CLIENT.name())
                .transportType("SCTP")
                .hostAddress("127.0.0.1")
                .hostPort(8010)
                .extraAddress("")
                .maxConcurrentConnections(0)
                .ss7M3uaId(1)
                .build();

        SettingsM3UA.Associations.Association association = SettingsM3UA.Associations.Association.builder()
                .id(1)
                .name("assoc")
                .state("STARTED")
                .peer("127.0.0.1")
                .enabled(1)
                .peerPort(8011)
                .m3uaHeartbeat(true)
                .m3uaSocketId(1)
                .aspName("ASP assoc")
                .build();

        SettingsM3UA.Route route = SettingsM3UA.Route.builder()
                .id(1)
                .originationPointCode(100)
                .destinationPointCode(200)
                .serviceIndicator(3)
                .trafficModeId(2)
                .appServers(List.of(1))
                .build();

        SettingsM3UA.ApplicationServer applicationServer = SettingsM3UA.ApplicationServer.builder()
                .id(1)
                .name("AS")
                .state("STARTED")
                .functionality("IPSP-CLIENT")
                .exchange("SE")
                .routingContext(101)
                .networkAppearance(102)
                .trafficModeId(2)
                .minimumAspForLoadshare(0)
                .aspFactories(List.of(1))
                .build();

        SettingsM3UA.Associations associations = SettingsM3UA.Associations.builder()
                .sockets(List.of(socket))
                .associationList(List.of(association))
                .build();

        SettingsM3UA settingsM3UA =  SettingsM3UA.builder()
                .general(m3uaGeneral)
                .associations(associations)
                .routes(List.of(route))
                .applicationServers(List.of(applicationServer))
                .build();

        gateway.setSettingsM3UA(settingsM3UA);


        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());

        assertTrue(sctpLayer.getSctpManagement().isStarted());
        //check general settings
        NettySctpManagementImpl sctpManagement = (NettySctpManagementImpl) sctpLayer.getSctpManagement();
        M3UAManagementImpl m3uaManagement = (M3UAManagementImpl) m3uaLayer.getMtp3UserPart();
        this.checkSctpInitConfig(sctpManagement, m3uaGeneral);
        this.checkSctpThresholdConfig(sctpManagement, m3uaGeneral);
        this.checkClientAssociationConfig(m3uaManagement, associations);
        this.checkM3uaInitConfig(m3uaManagement, m3uaGeneral);
        this.checkM3uaApplicationServersConfig(m3uaManagement, settingsM3UA);
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    @DisplayName("startLayer when gateway is client with default values then do it successfully")
    void startLayerWhenGatewayIsClientWithDefaultValuesThenDoItSuccessfully() {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        SettingsM3UA.General m3uaGeneral = SettingsM3UA.General.builder()
                .id(1)
                .networkId(4)
                .connectDelay(5000)
                .threadCount(1)
                .heartBeatTime(10000)
                .build();

        SettingsM3UA.Associations.Socket socket = SettingsM3UA.Associations.Socket.builder()
                .id(1)
                .name("socket")
                .state("STARTED")
                .enabled(1)
                .socketType(Ss7Utils.AssociationType.CLIENT.name())
                .transportType("SCTP")
                .hostAddress("127.0.0.1")
                .hostPort(8010)
                .extraAddress("")
                .maxConcurrentConnections(0)
                .ss7M3uaId(1)
                .build();

        SettingsM3UA.Associations.Association association = SettingsM3UA.Associations.Association.builder()
                .id(1)
                .name("assoc")
                .state("STARTED")
                .peer("127.0.0.1")
                .enabled(1)
                .peerPort(8011)
                .m3uaHeartbeat(true)
                .m3uaSocketId(1)
                .aspName("ASP assoc")
                .build();

        SettingsM3UA.Route route = SettingsM3UA.Route.builder()
                .id(1)
                .originationPointCode(100)
                .destinationPointCode(200)
                .serviceIndicator(3)
                .trafficModeId(2)
                .appServers(List.of(1))
                .build();

        SettingsM3UA.ApplicationServer applicationServer = SettingsM3UA.ApplicationServer.builder()
                .id(1)
                .name("AS")
                .state("STARTED")
                .functionality("IPSP-CLIENT")
                .exchange("SE")
                .routingContext(101)
                .networkAppearance(102)
                .trafficModeId(2)
                .minimumAspForLoadshare(0)
                .aspFactories(List.of(1))
                .build();

        SettingsM3UA.Associations associations = SettingsM3UA.Associations.builder()
                .sockets(List.of(socket))
                .associationList(List.of(association))
                .build();

        SettingsM3UA settingsM3UA =  SettingsM3UA.builder()
                .general(m3uaGeneral)
                .associations(associations)
                .routes(List.of(route))
                .applicationServers(List.of(applicationServer))
                .build();

        gateway.setSettingsM3UA(settingsM3UA);


        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());

        assertTrue(sctpLayer.getSctpManagement().isStarted());
        //check general settings
        NettySctpManagementImpl sctpManagement = (NettySctpManagementImpl) sctpLayer.getSctpManagement();
        M3UAManagementImpl m3uaManagement = (M3UAManagementImpl) m3uaLayer.getMtp3UserPart();

        this.checkSctpInitConfig(sctpManagement, m3uaGeneral);
        //Checking the thresholds values are not equals, since they are set by default
        assertNotEquals(m3uaGeneral.getCcDelayThreshold1(), sctpManagement.getCongControl_DelayThreshold_1());
        assertNotEquals(m3uaGeneral.getCcDelayThreshold2(), sctpManagement.getCongControl_DelayThreshold_2());
        assertNotEquals(m3uaGeneral.getCcDelayThreshold3(), sctpManagement.getCongControl_DelayThreshold_3());
        assertNotEquals(m3uaGeneral.getCcDelayBackToNormalThreshold1(), sctpManagement.getCongControl_BackToNormalDelayThreshold_1());
        assertNotEquals(m3uaGeneral.getCcDelayBackToNormalThreshold2(), sctpManagement.getCongControl_BackToNormalDelayThreshold_2());
        assertNotEquals(m3uaGeneral.getCcDelayBackToNormalThreshold3(), sctpManagement.getCongControl_BackToNormalDelayThreshold_3());
        assertEquals(m3uaGeneral.getConnectDelay(), sctpManagement.getConnectDelay());

        assertEquals(path, m3uaManagement.getPersistDir());
        assertNotNull(m3uaManagement.getTransportManagement());

        assertNotEquals(m3uaGeneral.getMaxSequenceNumber(), m3uaManagement.getMaxSequenceNumber());
        assertNotEquals(m3uaGeneral.getMaxForRoute(), m3uaManagement.getMaxAsForRoute());
        assertNotEquals(m3uaGeneral.getRoutingLabelFormat(), m3uaManagement.getRoutingLabelFormat().toString());
        assertNotEquals(m3uaGeneral.getRoutingKeyManagementEnabled(), m3uaManagement.getRoutingKeyManagementEnabled());
        assertNotEquals(m3uaGeneral.getUseLowestBitForLink(), m3uaManagement.isUseLsbForLinksetSelection());
        this.checkClientAssociationConfig(m3uaManagement, associations);
        this.checkM3uaApplicationServersConfig(m3uaManagement, settingsM3UA);

        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    @DisplayName("startLayer when gateway is server then do it successfully")
    void startLayerWhenGatewayIsServerThenDoItSuccessfully() {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        SettingsM3UA.General m3uaGeneral = SettingsM3UA.General.builder()
                .id(1)
                .networkId(4)
                .connectDelay(5000)
                .maxSequenceNumber(256)
                .maxForRoute(2)
                .threadCount(1)
                .routingLabelFormat("ITU")
                .heartBeatTime(10000)
                .routingKeyManagementEnabled(false)
                .useLowestBitForLink(false)
                .ccDelayThreshold1(2.6)
                .ccDelayThreshold2(2.5)
                .ccDelayThreshold3(3.0)
                .ccDelayBackToNormalThreshold1(3.4)
                .ccDelayBackToNormalThreshold2(4.5)
                .ccDelayBackToNormalThreshold3(5.0)
                .build();

        SettingsM3UA.Associations.Socket socket = SettingsM3UA.Associations.Socket.builder()
                .id(1)
                .name("socket")
                .state("STARTED")
                .enabled(1)
                .socketType(Ss7Utils.AssociationType.SERVER.name())
                .transportType("SCTP")
                .hostAddress("127.0.0.1")
                .hostPort(8010)
                .extraAddress("")
                .maxConcurrentConnections(0)
                .ss7M3uaId(1)
                .build();

        SettingsM3UA.Associations.Association association = SettingsM3UA.Associations.Association.builder()
                .id(1)
                .name("assoc")
                .state("STARTED")
                .peer("127.0.0.1")
                .enabled(1)
                .peerPort(8011)
                .m3uaHeartbeat(true)
                .m3uaSocketId(1)
                .aspName("ASP assoc")
                .build();

        SettingsM3UA.Route route = SettingsM3UA.Route.builder()
                .id(1)
                .originationPointCode(100)
                .destinationPointCode(200)
                .serviceIndicator(3)
                .trafficModeId(2)
                .appServers(List.of(1))
                .build();

        SettingsM3UA.ApplicationServer applicationServer = SettingsM3UA.ApplicationServer.builder()
                .id(1)
                .name("AS")
                .state("STARTED")
                .functionality("IPSP-SERVER")
                .exchange("SE")
                .routingContext(101)
                .networkAppearance(102)
                .trafficModeId(2)
                .minimumAspForLoadshare(0)
                .aspFactories(List.of(1))
                .build();

        SettingsM3UA.Associations associations = SettingsM3UA.Associations.builder()
                .sockets(List.of(socket))
                .associationList(List.of(association))
                .build();

        SettingsM3UA settingsM3UA =  SettingsM3UA.builder()
                .general(m3uaGeneral)
                .associations(associations)
                .routes(List.of(route))
                .applicationServers(List.of(applicationServer))
                .build();

        gateway.setSettingsM3UA(settingsM3UA);


        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());

        assertTrue(sctpLayer.getSctpManagement().isStarted());


        //check general settings
        M3UAManagementImpl m3uaManagement = (M3UAManagementImpl) m3uaLayer.getMtp3UserPart();
        NettySctpManagementImpl sctpManagement = (NettySctpManagementImpl) sctpLayer.getSctpManagement();
        this.checkSctpInitConfig(sctpManagement, m3uaGeneral);
        this.checkSctpThresholdConfig(sctpManagement, m3uaGeneral);
        this.checkSctpServerAssociationConfig(sctpManagement, associations);
        this.checkM3uaInitConfig(m3uaManagement, m3uaGeneral);
        this.checkM3uaApplicationServersConfig(m3uaManagement, settingsM3UA);
    }

    @Test
    @DisplayName("manageSocket and manageAssociation when gateway is client then do it successfully")
    void manageSocketAndAssociationWhenGatewayIsClientThenDoItSuccessfully() {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        SettingsM3UA.General m3uaGeneral = SettingsM3UA.General.builder()
                .id(1)
                .networkId(4)
                .connectDelay(5000)
                .maxSequenceNumber(256)
                .maxForRoute(2)
                .threadCount(1)
                .routingLabelFormat("ITU")
                .heartBeatTime(10000)
                .routingKeyManagementEnabled(false)
                .useLowestBitForLink(false)
                .ccDelayThreshold1(2.6)
                .ccDelayThreshold2(2.5)
                .ccDelayThreshold3(3.0)
                .ccDelayBackToNormalThreshold1(3.4)
                .ccDelayBackToNormalThreshold2(4.5)
                .ccDelayBackToNormalThreshold3(5.0)
                .build();

        SettingsM3UA.Associations.Socket socket = SettingsM3UA.Associations.Socket.builder()
                .id(1)
                .name("socket")
                .state("STARTED")
                .enabled(1)
                .socketType(Ss7Utils.AssociationType.CLIENT.name())
                .transportType("SCTP")
                .hostAddress("127.0.0.1")
                .hostPort(8010)
                .extraAddress("")
                .maxConcurrentConnections(0)
                .ss7M3uaId(1)
                .build();

        SettingsM3UA.Associations.Association association = SettingsM3UA.Associations.Association.builder()
                .id(1)
                .name("assoc")
                .state("STARTED")
                .peer("127.0.0.1")
                .enabled(1)
                .peerPort(8011)
                .m3uaHeartbeat(true)
                .m3uaSocketId(1)
                .aspName("ASP assoc")
                .build();

        SettingsM3UA.Route route = SettingsM3UA.Route.builder()
                .id(1)
                .originationPointCode(100)
                .destinationPointCode(200)
                .serviceIndicator(3)
                .trafficModeId(2)
                .appServers(List.of(1))
                .build();

        SettingsM3UA.ApplicationServer applicationServer = SettingsM3UA.ApplicationServer.builder()
                .id(1)
                .name("AS")
                .state("STARTED")
                .functionality("IPSP-CLIENT")
                .exchange("SE")
                .routingContext(101)
                .networkAppearance(102)
                .trafficModeId(2)
                .minimumAspForLoadshare(0)
                .aspFactories(List.of(1))
                .build();

        SettingsM3UA.Associations associations = SettingsM3UA.Associations.builder()
                .sockets(List.of(socket))
                .associationList(List.of(association))
                .build();

        SettingsM3UA settingsM3UA =  SettingsM3UA.builder()
                .general(m3uaGeneral)
                .associations(associations)
                .routes(List.of(route))
                .applicationServers(List.of(applicationServer))
                .build();

        gateway.setSettingsM3UA(settingsM3UA);


        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());

        assertTrue(sctpLayer.getSctpManagement().isStarted());
        //check Associations
        M3UAManagementImpl m3uaManagement = (M3UAManagementImpl) m3uaLayer.getMtp3UserPart();
        this.checkClientAssociationConfig(m3uaManagement, associations);


        //check Associations
        m3uaManagement.getTransportManagement().getAssociations().values().forEach(assoc -> {
            assertTrue(assoc.isStarted());
            sctpLayer.manageAssociation(assoc.getName(), false);
            assertFalse(assoc.isStarted());
            sctpLayer.manageAssociation(assoc.getName(), true);
            assertTrue(assoc.isStarted());
        });


        sctpLayer.manageSocket(1, false);

        m3uaManagement.getTransportManagement().getAssociations().values().forEach(assoc -> {
            assertFalse(assoc.isStarted());
        });


        sctpLayer.manageSocket(1, true);
        m3uaManagement.getTransportManagement().getAssociations().values().forEach(assoc -> {
            assertTrue(assoc.isStarted());
        });

        //Testing unknown socket
        sctpLayer.manageSocket(-1, true);
        sctpLayer.manageSocket(-1, false);

        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    @DisplayName("manageSocket and manageAssociation when gateway is server then do it successfully")
    void manageSocketWhenGatewayIsServerThenDoItSuccessfully() {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        SettingsM3UA.General m3uaGeneral = SettingsM3UA.General.builder()
                .id(1)
                .networkId(4)
                .connectDelay(5000)
                .maxSequenceNumber(256)
                .maxForRoute(2)
                .threadCount(1)
                .routingLabelFormat("ITU")
                .heartBeatTime(10000)
                .routingKeyManagementEnabled(false)
                .useLowestBitForLink(false)
                .ccDelayThreshold1(2.6)
                .ccDelayThreshold2(2.5)
                .ccDelayThreshold3(3.0)
                .ccDelayBackToNormalThreshold1(3.4)
                .ccDelayBackToNormalThreshold2(4.5)
                .ccDelayBackToNormalThreshold3(5.0)
                .build();

        SettingsM3UA.Associations.Socket socket = SettingsM3UA.Associations.Socket.builder()
                .id(1)
                .name("socket")
                .state("STARTED")
                .enabled(1)
                .socketType(Ss7Utils.AssociationType.SERVER.name())
                .transportType("SCTP")
                .hostAddress("127.0.0.1")
                .hostPort(8010)
                .extraAddress("")
                .maxConcurrentConnections(0)
                .ss7M3uaId(1)
                .build();

        SettingsM3UA.Associations.Association association = SettingsM3UA.Associations.Association.builder()
                .id(1)
                .name("assoc")
                .state("STARTED")
                .peer("127.0.0.1")
                .enabled(1)
                .peerPort(8011)
                .m3uaHeartbeat(true)
                .m3uaSocketId(1)
                .aspName("ASP assoc")
                .build();

        SettingsM3UA.Route route = SettingsM3UA.Route.builder()
                .id(1)
                .originationPointCode(100)
                .destinationPointCode(200)
                .serviceIndicator(3)
                .trafficModeId(2)
                .appServers(List.of(1))
                .build();

        SettingsM3UA.ApplicationServer applicationServer = SettingsM3UA.ApplicationServer.builder()
                .id(1)
                .name("AS")
                .state("STARTED")
                .functionality("IPSP-SERVER")
                .exchange("SE")
                .routingContext(101)
                .networkAppearance(102)
                .trafficModeId(2)
                .minimumAspForLoadshare(0)
                .aspFactories(List.of(1))
                .build();

        SettingsM3UA.Associations associations = SettingsM3UA.Associations.builder()
                .sockets(List.of(socket))
                .associationList(List.of(association))
                .build();

        SettingsM3UA settingsM3UA =  SettingsM3UA.builder()
                .general(m3uaGeneral)
                .associations(associations)
                .routes(List.of(route))
                .applicationServers(List.of(applicationServer))
                .build();

        gateway.setSettingsM3UA(settingsM3UA);


        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());

        assertTrue(sctpLayer.getSctpManagement().isStarted());
        //check general settings
        NettySctpManagementImpl sctpManagement = (NettySctpManagementImpl) sctpLayer.getSctpManagement();
        M3UAManagementImpl m3uaManagement = (M3UAManagementImpl) m3uaLayer.getMtp3UserPart();
        this.checkSctpInitConfig(sctpManagement, m3uaGeneral);
        this.checkSctpThresholdConfig(sctpManagement, m3uaGeneral);
        this.checkM3uaInitConfig(m3uaManagement, m3uaGeneral);
        this.checkM3uaApplicationServersConfig(m3uaManagement, settingsM3UA);

        m3uaManagement.getTransportManagement().getAssociations().values().forEach(assoc -> {
            assertTrue(assoc.isStarted());
        });


        sctpLayer.manageSocket(1, false);

        m3uaManagement.getTransportManagement().getAssociations().values().forEach(assoc -> {
            assertFalse(assoc.isStarted());
        });

        sctpLayer.manageSocket(1, true);

        m3uaManagement.getTransportManagement().getAssociations().values().forEach(assoc -> {
            assertTrue(assoc.isStarted());
        });


        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    private void checkSctpInitConfig(NettySctpManagementImpl sctpManagement, SettingsM3UA.General m3uaGeneral) {
        assertEquals(path, sctpManagement.getPersistDir());
        assertEquals(m3uaGeneral.getThreadCount() == 1, sctpManagement.isSingleThread());
        assertEquals(m3uaGeneral.getThreadCount(), sctpManagement.getWorkerThreads());
        assertEquals(m3uaGeneral.getThreadCount(), sctpManagement.getBossGroupThreadCount());
        assertEquals(m3uaGeneral.getThreadCount(), sctpManagement.getWorkerGroupThreadCount());
    }

    private void checkSctpThresholdConfig(NettySctpManagementImpl sctpManagement, SettingsM3UA.General m3uaGeneral) {
        assertEquals(m3uaGeneral.getCcDelayThreshold1(), sctpManagement.getCongControl_DelayThreshold_1());
        assertEquals(m3uaGeneral.getCcDelayThreshold2(), sctpManagement.getCongControl_DelayThreshold_2());
        assertEquals(m3uaGeneral.getCcDelayThreshold3(), sctpManagement.getCongControl_DelayThreshold_3());
        assertEquals(m3uaGeneral.getCcDelayBackToNormalThreshold1(), sctpManagement.getCongControl_BackToNormalDelayThreshold_1());
        assertEquals(m3uaGeneral.getCcDelayBackToNormalThreshold2(), sctpManagement.getCongControl_BackToNormalDelayThreshold_2());
        assertEquals(m3uaGeneral.getCcDelayBackToNormalThreshold3(), sctpManagement.getCongControl_BackToNormalDelayThreshold_3());
        assertEquals(m3uaGeneral.getConnectDelay(), sctpManagement.getConnectDelay());
    }

    private void checkClientAssociationConfig(M3UAManagementImpl m3UAManagement, SettingsM3UA.Associations associations) {
        var associationsMap = m3UAManagement.getTransportManagement().getAssociations();
        var aspFactoriesMap = m3UAManagement.getAspfactories().stream().collect(Collectors.toMap(AspFactory::getName, asp -> asp));
        assertEquals(associations.getAssociationList().size(), associationsMap.size());
        if (!associations.getAssociationList().isEmpty()) {
            associations.getAssociationList().forEach(assoc  -> {
                Association association = associationsMap.get(assoc.getName());
                AspFactory aspFactory = aspFactoriesMap.get(assoc.getAspName());
                assertNotNull(association);
                assertNotNull(aspFactory);
                assertEquals(assoc.getName(), association.getName());
                assertEquals(assoc.getPeer(), association.getPeerAddress());
                assertEquals(assoc.getPeerPort(), association.getPeerPort());
                var socketAssoc = associations.getSockets().stream().filter(socket -> socket.getId() == assoc.getM3uaSocketId()).findFirst().orElse(null);
                assertNotNull(socketAssoc);
                assertEquals(socketAssoc.getHostAddress(), association.getHostAddress());
                assertEquals(socketAssoc.getHostPort(), association.getHostPort());
                assertEquals(socketAssoc.getTransportType(), association.getIpChannelType().name());
                assertTrue(association.isStarted());

                // check asp factory
                assertEquals(assoc.getAspName(), aspFactory.getName());
                assertEquals(assoc.isM3uaHeartbeat(), aspFactory.isHeartBeatEnabled());
            });
        }
    }

    private void checkSctpServerAssociationConfig(NettySctpManagementImpl sctpManagement, SettingsM3UA.Associations associations) {
        var associationsMap = sctpManagement.getAssociations();
        assertEquals(associations.getAssociationList().size(), associationsMap.size());
        if (!associations.getAssociationList().isEmpty()) {
            associations.getAssociationList().forEach(assoc  -> {
                Association associationServer = associationsMap.get(assoc.getName());
                assertEquals(assoc.getName(), associationServer.getName());
                assertEquals(assoc.getPeer(), associationServer.getPeerAddress());
                assertEquals(assoc.getPeerPort(), associationServer.getPeerPort());
                var socketAssoc = associations.getSockets().stream().filter(socket -> socket.getId() == assoc.getM3uaSocketId()).findFirst().orElse(null);
                assertNotNull(socketAssoc);
                assertEquals(socketAssoc.getTransportType(), associationServer.getIpChannelType().name());
            });
        }
    }

    private void checkM3uaInitConfig(M3UAManagementImpl m3uaManagement, SettingsM3UA.General m3uaGeneral) {
        assertEquals(path, m3uaManagement.getPersistDir());
        assertEquals(m3uaGeneral.getThreadCount() == 1, m3uaManagement.getTransportManagement().isSingleThread());
        assertNotNull(m3uaManagement.getTransportManagement());
        assertEquals(m3uaGeneral.getThreadCount(), m3uaManagement.getDeliveryMessageThreadCount());
        assertEquals(m3uaGeneral.getMaxSequenceNumber(), m3uaManagement.getMaxSequenceNumber());
        assertEquals(m3uaGeneral.getMaxForRoute(), m3uaManagement.getMaxAsForRoute());
        assertEquals(m3uaGeneral.getRoutingLabelFormat(), m3uaManagement.getRoutingLabelFormat().toString());
        assertEquals(m3uaGeneral.getRoutingKeyManagementEnabled(), m3uaManagement.getRoutingKeyManagementEnabled());
        assertEquals(m3uaGeneral.getUseLowestBitForLink(), m3uaManagement.isUseLsbForLinksetSelection());
        assertEquals(m3uaGeneral.getHeartBeatTime(), m3uaManagement.getHeartbeatTime());
    }

    private void checkM3uaApplicationServersConfig(M3UAManagementImpl m3uaManagement, SettingsM3UA settingsM3UA) {
        var applicationServerMaps = m3uaManagement.getAppServers().stream().collect(Collectors.toMap(As::getName, as -> as));
        settingsM3UA.getApplicationServers().forEach(as -> {
            assertTrue(applicationServerMaps.containsKey(as.getName()));
            As applicationServer = applicationServerMaps.get(as.getName());
            assertEquals(as.getName(), applicationServer.getName());
            String functionality = as.getFunctionality().split("-")[0];
            String ipsType = "";
            if (as.getFunctionality().split("-").length > 1) {
                ipsType = as.getFunctionality().split("-")[1];
            }

            assertEquals(functionality.toUpperCase(), applicationServer.getFunctionality().name());
            assertEquals(as.getExchange().toUpperCase(), applicationServer.getExchangeType().name());
            assertEquals(ipsType.toUpperCase(), applicationServer.getIpspType().name());
            assertEquals(as.getRoutingContext(), applicationServer.getRoutingContext().getRoutingContexts()[0]);
            assertEquals(as.getTrafficModeId(), applicationServer.getTrafficModeType().getMode());
            assertEquals(as.getMinimumAspForLoadshare(), applicationServer.getMinAspActiveForLb());

            if (Objects.isNull(as.getNetworkAppearance())) {
                assertNull(applicationServer.getNetworkAppearance());
            } else {
                assertEquals(as.getNetworkAppearance().longValue(), applicationServer.getNetworkAppearance().getNetApp());
            }
        });
    }

}