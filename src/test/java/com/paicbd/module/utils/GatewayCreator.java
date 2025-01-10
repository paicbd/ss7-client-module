package com.paicbd.module.utils;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsM3UA;
import com.paicbd.module.dto.SettingsMAP;
import com.paicbd.module.dto.SettingsSCCP;
import com.paicbd.module.dto.SettingsTCAP;
import com.paicbd.module.dto.statics.AddressConfig;
import com.paicbd.module.dto.statics.GeneralSCCP;
import com.paicbd.module.dto.statics.Mtp3DestinationConfig;
import com.paicbd.module.dto.statics.RemoteResourceConfig;
import com.paicbd.module.dto.statics.RuleConfig;
import com.paicbd.module.dto.statics.ServiceAccessConfig;
import com.paicbd.module.dto.statics.ServiceAccessPointsConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.util.List;

@Slf4j
public class GatewayCreator {
    /*
    ------------------------------------------------------------------------------------------------------
    | The following methods are used to create the Gateway, SettingsM3UA, SettingsSCCP, and SettingsTCAP |
    | objects for testing purposes.                                                                      |
    ------------------------------------------------------------------------------------------------------
     */
    public static Gateway buildSS7Gateway(String name, int mnoId, int networkId) {
        return buildSS7Gateway(name, mnoId, networkId, GatewayCreator.getRandomLocalPort(), GatewayCreator.getRandomLocalPort());
    }

    public static Gateway buildSS7Gateway(String name, int mnoId, int networkId, int hostPort, int peerPort) {
        return Gateway.builder()
                .name(name)
                .mnoId(mnoId)
                .enabled(1)
                .networkId(networkId)
                .settingsM3UA(buildM3UASettings(networkId, hostPort, peerPort))
                .settingsSCCP(buildSCCPSettings(networkId))
                .settingsTCAP(buildTCAPSettings(networkId))
                .settingsMAP(SettingsMAP.builder()
                        .id(1)
                        .networkId(networkId)
                        .sriServiceOpCode(45)
                        .forwardSmServiceOpCode(44)
                        .build()
                )
                .build();
    }

    public static SettingsTCAP buildTCAPSettings(int networkId) {
        return SettingsTCAP.builder()
                .id(1)
                .networkId(networkId)
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
                .build();
    }

    public static SettingsM3UA buildM3UASettings(int networkId, int hostPort, int peerPort) {
        SettingsM3UA.General m3uaGeneral = SettingsM3UA.General.builder()
                .id(1)
                .networkId(networkId)
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
                .state("STOPPED")
                .enabled(0)
                .socketType("Client")
                .transportType("SCTP")
                .hostAddress("127.0.0.1")
                .hostPort(hostPort)
                .extraAddress("")
                .maxConcurrentConnections(0)
                .ss7M3uaId(1)
                .build();

        SettingsM3UA.Associations.Association association = SettingsM3UA.Associations.Association.builder()
                .id(1)
                .name("assoc")
                .state("STOPPED")
                .peer("127.0.0.1")
                .enabled(0)
                .peerPort(peerPort)
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

        return SettingsM3UA.builder()
                .general(m3uaGeneral)
                .associations(associations)
                .routes(List.of(route))
                .applicationServers(List.of(applicationServer))
                .build();
    }

    public static SettingsSCCP buildSCCPSettings(int networkId) {
        return SettingsSCCP.builder()
                .generalSCCP(GeneralSCCP.builder()
                        .id(1)
                        .networkId(networkId)
                        .zMarginXudtMessage(240)
                        .removeSpc(true)
                        .sstTimerDurationMin(10000)
                        .sstTimerDurationMax(6000000)
                        .sstTimerDurationIncreaseFactor(1.0)
                        .maxDataMessage(2560)
                        .periodOfLogging(60000)
                        .reassemblyTimerDelay(15000)
                        .previewMode(false)
                        .sccpProtocolVersion("ITU")
                        .congestionControlTimerA(400)
                        .congestionControlTimerD(2000)
                        .congestionControlAlgorithm("international")
                        .congestionControl(false)
                        .build())
                .addresses(
                        List.of(
                                AddressConfig.builder()
                                        .id(1)
                                        .name("address100")
                                        .digits("*")
                                        .addressIndicator(17)
                                        .pointCode(100)
                                        .subsystemNumber(0)
                                        .gtIndicator("GT0100")
                                        .translationType(0)
                                        .numberingPlanId(1)
                                        .natureOfAddressId(4)
                                        .ss7SccpId(1)
                                        .build(),
                                        AddressConfig.builder()
                                        .id(2)
                                        .name("address200")
                                        .digits("*")
                                        .addressIndicator(17)
                                        .pointCode(200)
                                        .subsystemNumber(0)
                                        .gtIndicator("GT0100")
                                        .translationType(0)
                                        .numberingPlanId(1)
                                        .natureOfAddressId(4)
                                        .ss7SccpId(1)
                                        .build()
                        ))
                .rules(
                        List.of(
                                        RuleConfig.builder()
                                        .id(1)
                                        .name("100")
                                        .mask("K")
                                        .calledAddressIndicator(16)
                                        .calledPointCode(0)
                                        .calledSubsystemNumber(0)
                                        .calledGtIndicator("GT0100")
                                        .calledTranslationType(0)
                                        .calledNumberingPlanId(1)
                                        .calledNatureOfAddressId(4)
                                        .calledGlobalTittleDigits("*")
                                        .ruleTypeId(1)
                                        .primaryAddressId(1)
                                        .loadSharingAlgorithmId(1)
                                        .originationTypeId(3)
                                        .callingNumberingPlanId(-1)
                                        .callingNatureOfAddressId(-1)
                                        .build(),
                                        RuleConfig.builder()
                                        .id(2)
                                        .name("200")
                                        .mask("K")
                                        .calledAddressIndicator(16)
                                        .calledPointCode(0)
                                        .calledSubsystemNumber(0)
                                        .calledGtIndicator("GT0100")
                                        .calledTranslationType(0)
                                        .calledNumberingPlanId(1)
                                        .calledNatureOfAddressId(4)
                                        .calledGlobalTittleDigits("*")
                                        .ruleTypeId(1)
                                        .primaryAddressId(2)
                                        .loadSharingAlgorithmId(1)
                                        .originationTypeId(2)
                                        .callingNumberingPlanId(-1)
                                        .callingNatureOfAddressId(-1)
                                        .build()
                        )
                )
                .remoteResources(
                        List.of(
                                RemoteResourceConfig.builder()
                                        .id(1)
                                        .remoteSpc(200)
                                        .remoteSpcStatus("ALLOWED")
                                        .remoteSccpStatus("ALLOWED")
                                        .remoteSsn(6)
                                        .remoteSsnStatus("ALLOWED")
                                        .markProhibited(false)
                                        .ss7SccpId(1)
                                        .build(),
                                    RemoteResourceConfig.builder()
                                        .id(2)
                                        .remoteSpc(200)
                                        .remoteSpcStatus("ALLOWED")
                                        .remoteSccpStatus("ALLOWED")
                                        .remoteSsn(8)
                                        .remoteSsnStatus("ALLOWED")
                                        .markProhibited(false)
                                        .ss7SccpId(1)
                                        .build()
                        )
                )
                .serviceAccessPoints(
                        ServiceAccessPointsConfig.builder()
                                .serviceAccess(
                                        List.of(
                                                ServiceAccessConfig.builder()
                                                        .id(1)
                                                        .name("sap1")
                                                        .originPointCode(100)
                                                        .networkIndicator(2)
                                                        .localGtDigits("")
                                                        .ss7SccpId(1)
                                                        .build()
                                        )
                                )
                                .mtp3Destinations(
                                        List.of(
                                                Mtp3DestinationConfig.builder()
                                                        .id(1)
                                                        .name("mtp3_1")
                                                        .firstPointCode(200)
                                                        .lastPointCode(200)
                                                        .firstSls(0)
                                                        .lastSls(255)
                                                        .slsMask(255)
                                                        .sccpSapId(1)
                                                        .build()
                                        )
                                )
                                .build()
                ).build();
    }

    public static int getRandomLocalPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (Exception e) {
            log.error("Error while creating server socket", e);
            return 0;
        }
    }
}
