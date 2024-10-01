package com.paicbd.module.ss7.layer.impl;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsM3UA;
import com.paicbd.module.dto.SettingsMAP;
import com.paicbd.module.dto.SettingsSCCP;
import com.paicbd.module.dto.SettingsTCAP;

import java.util.ArrayList;
import java.util.List;

public class GatewayUtil {

    public static Gateway getGateway(int hostPort, int peerPort) {
        Gateway ss7Gateway = new Gateway();
        ss7Gateway.setName("ss7Test");
        ss7Gateway.setNetworkId(1);
        ss7Gateway.setMnoId(1);
        ss7Gateway.setSettingsM3UA(getSettingsM3UA(hostPort, peerPort));
        ss7Gateway.setSettingsSCCP(getSettingsSCCP());
        ss7Gateway.setSettingsTCAP(getSettingsTCAP());
        ss7Gateway.setSettingsMAP(getSettingsMAP());
        return ss7Gateway;
    }

    private static SettingsM3UA getSettingsM3UA(int hostPort, int peerPort) {
        SettingsM3UA settingsM3UA = new SettingsM3UA();
        settingsM3UA.setGeneral(getSettingsM3UAGeneral());
        settingsM3UA.setAssociations(getSettingsM3UAAssociations(hostPort, peerPort));
        settingsM3UA.setApplicationServers(List.of(getSettingsM3UAApplicationServer()));
        settingsM3UA.setRoutes(List.of(getSettingsM3UARoute()));
        return settingsM3UA;
    }


    private static SettingsM3UA.General getSettingsM3UAGeneral() {
        SettingsM3UA.General settingsM3UAGeneral = new SettingsM3UA.General();
        settingsM3UAGeneral.setId(1);
        settingsM3UAGeneral.setNetworkId(1);
        settingsM3UAGeneral.setConnectDelay(5000);
        settingsM3UAGeneral.setMaxSequenceNumber(256);
        settingsM3UAGeneral.setMaxForRoute(2);
        settingsM3UAGeneral.setThreadCount(2);
        settingsM3UAGeneral.setRoutingLabelFormat("ITU");
        settingsM3UAGeneral.setHeartBeatTime(10000);
        settingsM3UAGeneral.setRoutingKeyManagementEnabled(false);
        settingsM3UAGeneral.setUseLowestBitForLink(false);
        settingsM3UAGeneral.setCcDelayThreshold1(2.6);
        settingsM3UAGeneral.setCcDelayThreshold2(2.5);
        settingsM3UAGeneral.setCcDelayThreshold3(3.0);
        settingsM3UAGeneral.setCcDelayBackToNormalThreshold1(3.4);
        settingsM3UAGeneral.setCcDelayBackToNormalThreshold2(4.5);
        settingsM3UAGeneral.setCcDelayBackToNormalThreshold3(5.0);
        return settingsM3UAGeneral;
    }

    private static SettingsM3UA.Associations getSettingsM3UAAssociations(int hostPort, int peerPort) {
        SettingsM3UA.Associations associations = new SettingsM3UA.Associations();
        SettingsM3UA.Associations.Socket socket = new SettingsM3UA.Associations.Socket();
        socket.setId(1);
        socket.setName("socket");
        socket.setState("STOPPED");
        socket.setEnabled(0);
        socket.setSocketType("Client");
        socket.setTransportType("SCTP");
        socket.setHostAddress("127.0.0.1");
        socket.setHostPort(hostPort);
        socket.setExtraAddress("");
        socket.setMaxConcurrentConnections(0);
        socket.setSs7M3uaId(1);

        SettingsM3UA.Associations.Association association = new SettingsM3UA.Associations.Association();
        association.setId(1);
        association.setName("assoc");
        association.setState("ACTIVE");
        association.setEnabled(0);
        association.setPeer("127.0.0.1");
        association.setPeerPort(peerPort);
        association.setM3uaHeartbeat(true);
        association.setM3uaSocketId(1);
        association.setAspName("ASP assoc");

        associations.setAssociationList(List.of(association));
        associations.setSockets(List.of(socket));
        return associations;
    }

    private static SettingsM3UA.ApplicationServer getSettingsM3UAApplicationServer() {
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
        return appServer;

    }

    private static SettingsM3UA.Route getSettingsM3UARoute() {
        SettingsM3UA.Route route = new SettingsM3UA.Route();
        route.setId(1);
        route.setOriginationPointCode(100);
        route.setDestinationPointCode(200);
        route.setServiceIndicator(3);
        route.setTrafficModeId(2);
        route.setAppServers(List.of(1));
        return route;
    }

    private static SettingsSCCP getSettingsSCCP() {
        SettingsSCCP sccp = new SettingsSCCP();
        sccp.setGeneral(getSettingsSCCPGeneral());
        sccp.setAddresses(getSettingsSCCPAddressConfigs());
        sccp.setRules(getSettingsSCCPRuleConfigs());
        sccp.setRemoteResources(getSettingsSCCPRemoteResourceConfigs());
        sccp.setServiceAccessPoints(getServiceAccessPointsConfig());
        return sccp;
    }

    private static SettingsSCCP.General getSettingsSCCPGeneral() {
        SettingsSCCP.General settingsSCCPGeneral = new SettingsSCCP.General();
        settingsSCCPGeneral.setId(1);
        settingsSCCPGeneral.setNetworkId(1);
        settingsSCCPGeneral.setZMarginXudtMessage(240);
        settingsSCCPGeneral.setRemoveSpc(true);
        settingsSCCPGeneral.setSstTimerDurationMin(10000);
        settingsSCCPGeneral.setSstTimerDurationMax(6000000);
        settingsSCCPGeneral.setSstTimerDurationIncreaseFactor(1.0);
        settingsSCCPGeneral.setMaxDataMessage(2560);
        settingsSCCPGeneral.setPeriodOfLogging(60000);
        settingsSCCPGeneral.setReassemblyTimerDelay(15000);
        settingsSCCPGeneral.setPreviewMode(false);
        settingsSCCPGeneral.setSccpProtocolVersion("ITU");
        settingsSCCPGeneral.setCongestionControlTimerA(400);
        settingsSCCPGeneral.setCongestionControlTimerD(2000);
        settingsSCCPGeneral.setCongestionControlAlgorithm("international");
        settingsSCCPGeneral.setCongestionControl(false);
        return settingsSCCPGeneral;
    }

    private static List<SettingsSCCP.AddressConfig> getSettingsSCCPAddressConfigs() {
        List<SettingsSCCP.AddressConfig> addressConfigs = new ArrayList<>();
        addressConfigs.add(getAddressConfig(3, "address100", 100, 1));
        addressConfigs.add(getAddressConfig(4, "address200", 200, 1));
        return addressConfigs;
    }

    private static SettingsSCCP.AddressConfig getAddressConfig(int id, String name, int pointCode, int sccpId) {
        var addressConfig = new SettingsSCCP.AddressConfig();
        addressConfig.setId(id);
        addressConfig.setName(name);
        addressConfig.setDigits("*");
        addressConfig.setAddressIndicator(17);
        addressConfig.setPointCode(pointCode);
        addressConfig.setSubsystemNumber(0);
        addressConfig.setGtIndicator("GT0100");
        addressConfig.setTranslationType(0);
        addressConfig.setNumberingPlanId(1);
        addressConfig.setNatureOfAddressId(4);
        addressConfig.setSs7SccpId(sccpId);
        return addressConfig;
    }

    private static List<SettingsSCCP.RuleConfig> getSettingsSCCPRuleConfigs() {
        List<SettingsSCCP.RuleConfig> ruleConfigs = new ArrayList<>();
        ruleConfigs.add(getRuleConfig(3, "100", 3, 3));
        ruleConfigs.add(getRuleConfig(4, "200", 4, 2));
        return ruleConfigs;
    }

    public static SettingsSCCP.RuleConfig getRuleConfig(int id, String name, int idAddress,
                                                         int originationTypeId) {
        SettingsSCCP.RuleConfig ruleConfig = new SettingsSCCP.RuleConfig();
        ruleConfig.setId(id);
        ruleConfig.setName(name);
        ruleConfig.setMask("K");
        ruleConfig.setCalledAddressIndicator(16);
        ruleConfig.setCalledPointCode(0);
        ruleConfig.setCalledSubsystemNumber(0);
        ruleConfig.setCalledGtIndicator("GT0100");
        ruleConfig.setCalledTranslationType(0);
        ruleConfig.setCalledNumberingPlanId(1);
        ruleConfig.setCalledNatureOfAddressId(4);
        ruleConfig.setCalledGlobalTittleDigits("*");
        ruleConfig.setRuleTypeId(1);
        ruleConfig.setPrimaryAddressId(idAddress);
        ruleConfig.setSecondaryAddressId(null);
        ruleConfig.setLoadSharingAlgorithmId(1);
        ruleConfig.setOriginationTypeId(originationTypeId);
        ruleConfig.setNewCallingPartyAddress(null);
        return ruleConfig;
    }

    private static List<SettingsSCCP.RemoteResourceConfig> getSettingsSCCPRemoteResourceConfigs() {
        List<SettingsSCCP.RemoteResourceConfig> remoteResourceConfigs = new ArrayList<>();
        remoteResourceConfigs.add(getRemoteResourceConfig(3, 6));
        remoteResourceConfigs.add(getRemoteResourceConfig(4, 8));
        return remoteResourceConfigs;
    }

    private static SettingsSCCP.RemoteResourceConfig getRemoteResourceConfig(int id, int ssn) {
        SettingsSCCP.RemoteResourceConfig remoteResourceConfig = new SettingsSCCP.RemoteResourceConfig();
        remoteResourceConfig.setId(id);
        remoteResourceConfig.setRemoteSpc(200);
        remoteResourceConfig.setRemoteSpcStatus("ALLOWED");
        remoteResourceConfig.setRemoteSccpStatus("ALLOWED");
        remoteResourceConfig.setRemoteSsn(ssn);
        remoteResourceConfig.setRemoteSsnStatus("ALLOWED");
        remoteResourceConfig.setMarkProhibited(false);
        remoteResourceConfig.setSs7SccpId(1);
        return remoteResourceConfig;
    }

    private static SettingsSCCP.ServiceAccessPointsConfig getServiceAccessPointsConfig() {
        SettingsSCCP.ServiceAccessPointsConfig serviceAccessPointsConfig = new SettingsSCCP.ServiceAccessPointsConfig();
        SettingsSCCP.ServiceAccessConfig serviceAccessConfig = new SettingsSCCP.ServiceAccessConfig();
        serviceAccessConfig.setId(1);
        serviceAccessConfig.setName("sap1");
        serviceAccessConfig.setOriginPointCode(100);
        serviceAccessConfig.setNetworkIndicator(2);
        serviceAccessConfig.setLocalGtDigits("");
        serviceAccessConfig.setSs7SccpId(1);
        serviceAccessPointsConfig.setServiceAccess(List.of(serviceAccessConfig));

        SettingsSCCP.Mtp3DestinationConfig mtp3DestinationConfig = new SettingsSCCP.Mtp3DestinationConfig();
        mtp3DestinationConfig.setId(1);
        mtp3DestinationConfig.setName("mtp3");
        mtp3DestinationConfig.setFirstPointCode(200);
        mtp3DestinationConfig.setLastPointCode(200);
        mtp3DestinationConfig.setFirstSls(0);
        mtp3DestinationConfig.setLastSls(255);
        mtp3DestinationConfig.setSlsMask(255);
        mtp3DestinationConfig.setSccpSapId(1);

        serviceAccessPointsConfig.setMtp3Destinations(List.of(mtp3DestinationConfig));

        return serviceAccessPointsConfig;
    }

    public static SettingsTCAP getSettingsTCAP() {
        SettingsTCAP settingsTCAP = new SettingsTCAP();
        settingsTCAP.setId(1);
        settingsTCAP.setNetworkId(1);
        settingsTCAP.setSsnList("8");
        settingsTCAP.setPreviewMode(false);
        settingsTCAP.setDialogIdleTimeout(100000);
        settingsTCAP.setInvokeTimeout(25000);
        settingsTCAP.setDialogIdRangeStart(1);
        settingsTCAP.setDialogIdRangeEnd(2147483647);
        settingsTCAP.setMaxDialogs(5000);
        settingsTCAP.setDoNotSendProtocolVersion(false);
        settingsTCAP.setSwapTcapIdEnabled(true);
        settingsTCAP.setSlsRangeId("All");
        settingsTCAP.setExrDelayThr1(1.0);
        settingsTCAP.setExrDelayThr2(6.0);
        settingsTCAP.setExrDelayThr3(12.0);
        settingsTCAP.setExrBackToNormalDelayThr1(0.5);
        settingsTCAP.setExrBackToNormalDelayThr2(3.0);
        settingsTCAP.setExrBackToNormalDelayThr3(8.0);
        settingsTCAP.setMemoryMonitorThr1(77.0);
        settingsTCAP.setMemoryMonitorThr2(87.0);
        settingsTCAP.setMemoryMonitorThr3(97.0);
        settingsTCAP.setMemBackToNormalDelayThr1(72);
        settingsTCAP.setMemBackToNormalDelayThr2(82);
        settingsTCAP.setMemBackToNormalDelayThr3(92);
        settingsTCAP.setBlockingIncomingTcapMessages(false);
        return settingsTCAP;
    }

    public static SettingsMAP getSettingsMAP() {
        SettingsMAP settingsMAP = new SettingsMAP();
        settingsMAP.setId(1);
        settingsMAP.setNetworkId(1);
        settingsMAP.setSriServiceOpCode(45);
        settingsMAP.setForwardSmServiceOpCode(44);
        return settingsMAP;
    }


}
