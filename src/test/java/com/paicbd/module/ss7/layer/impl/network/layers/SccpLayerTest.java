package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsSCCP;
import com.paicbd.module.dto.statics.AddressConfig;
import com.paicbd.module.dto.statics.GeneralSCCP;
import com.paicbd.module.dto.statics.Mtp3DestinationConfig;
import com.paicbd.module.dto.statics.RemoteResourceConfig;
import com.paicbd.module.dto.statics.RuleConfig;
import com.paicbd.module.dto.statics.ServiceAccessConfig;
import com.paicbd.module.dto.statics.ServiceAccessPointsConfig;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.utils.UtilsEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.sccp.RemoteSignalingPointCode;
import org.restcomm.protocols.ss7.sccp.SccpStack;
import org.restcomm.protocols.ss7.sccpext.impl.SccpExtModuleImpl;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SccpLayerTest {

    @Mock
    AppProperties appProperties;

    @Mock
    ExtendedResource extendedResource;

    String path;

    SctpLayer sctpLayer;

    M3uaLayer m3uaLayer;

    SccpLayer sccpLayer;


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
    @MethodSource("sccpSettingsToTest")
    @DisplayName("Start Layer when gateway is correct then do it successfully")
    void startLayerWithDifferentSccpSettingsThenDoItSuccessfully(SettingsSCCP settingsSCCP) {
        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        path = extendedResource.createDirectory(gateway.getName());
        gateway.setSettingsSCCP(settingsSCCP);

        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, gateway, path);

        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, gateway, path, sctpLayer);

        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(
                gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, gateway, path, m3uaLayer);

        assertDoesNotThrow(() -> sctpLayer.start());
        assertDoesNotThrow(() -> m3uaLayer.start());
        assertDoesNotThrow(() -> sccpLayer.start());

        assertTrue(sccpLayer.getSccpProvider().getSccpStack().isStarted());
        assertNotNull(sccpLayer.getSccpProvider().getSccpStack().getPersistDir());
        assertEquals(path, sccpLayer.getSccpProvider().getSccpStack().getPersistDir());

        SccpStack sccpStack = sccpLayer.getSccpProvider().getSccpStack();
        this.checkSccpRemoteResources(sccpStack, gateway.getSettingsSCCP());
        this.checkSccpServiceAccessPointsAndMtp3Destinations(sccpStack, gateway.getSettingsSCCP());
        this.checkSccpAddresses(sccpStack, gateway.getSettingsSCCP());
        this.checkSccpRules(sccpStack, gateway.getSettingsSCCP());

        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    static Stream<SettingsSCCP> sccpSettingsToTest() {
        var general = GeneralSCCP.builder()
                .id(1)
                .networkId(4)
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
                .build();

        var remoteResources = List.of(
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
        );

        return Stream.of(
                // SCCP Settings without calling data
                SettingsSCCP.builder()
                        .generalSCCP(general)
                        .addresses(
                                List.of(
                                        AddressConfig.builder()
                                                .id(10)
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
                                                .id(20)
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
                                                .primaryAddressId(10)
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
                                                .primaryAddressId(20)
                                                .loadSharingAlgorithmId(1)
                                                .originationTypeId(2)
                                                .callingNumberingPlanId(-1)
                                                .callingNatureOfAddressId(-1)
                                                .build()
                                )
                        )
                        .remoteResources(remoteResources)
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
                        ).build(),
                // SCCP Settings with calling data
                    SettingsSCCP.builder()
                        .generalSCCP(general)
                        .addresses(
                                List.of(
                                        AddressConfig.builder()
                                                .id(10)
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
                                                .id(20)
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
                                                .primaryAddressId(10)
                                                .loadSharingAlgorithmId(1)
                                                .originationTypeId(3)
                                                .callingNumberingPlanId(1)
                                                .callingNatureOfAddressId(4)
                                                .callingGlobalTittleDigits("*")
                                                .callingTranslationType(0)
                                                .callingGtIndicator("GT0100")
                                                .callingSubsystemNumber(0)
                                                .callingPointCode(0)
                                                .callingAddressIndicator(16)
                                                .newCallingPartyAddress("555555")
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
                                                .primaryAddressId(20)
                                                .loadSharingAlgorithmId(1)
                                                .originationTypeId(2)
                                                .callingNumberingPlanId(1)
                                                .callingNatureOfAddressId(4)
                                                .callingGlobalTittleDigits("*")
                                                .callingTranslationType(0)
                                                .callingGtIndicator("GT0100")
                                                .callingSubsystemNumber(0)
                                                .callingPointCode(0)
                                                .callingAddressIndicator(16)
                                                .newCallingPartyAddress("777777")
                                                .build()
                                )
                        )
                        .remoteResources(remoteResources)
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
                        ).build()
        );
    }

    private void checkSccpRemoteResources(SccpStack sccpStack, SettingsSCCP settingsSCCP) {
        var remoteSpscList = sccpStack.getSccpResource().getRemoteSpcs().values().stream().map(RemoteSignalingPointCode::getRemoteSpc).toList();

        List<Integer> remoteSpcList = settingsSCCP.getRemoteResources().stream()
                .map(RemoteResourceConfig::getRemoteSpc)
                .distinct()
                .toList();

        remoteSpcList.forEach(remoteSpc -> assertTrue(remoteSpscList.contains(remoteSpc)));

    }

    private void checkSccpServiceAccessPointsAndMtp3Destinations(SccpStack sccpStack, SettingsSCCP settingsSCCP) {
        var serviceAccessPointsMap = sccpStack.getRouter().getMtp3ServiceAccessPoints();

        settingsSCCP.getServiceAccessPoints().getServiceAccess().forEach(serviceAccessConfig -> {
            assertTrue(serviceAccessPointsMap.containsKey(serviceAccessConfig.getId()));
            var mtp3ServiceAccessPoint = serviceAccessPointsMap.get(serviceAccessConfig.getId());
            assertEquals(serviceAccessConfig.getId(), mtp3ServiceAccessPoint.getMtp3Id());
            assertEquals(serviceAccessConfig.getOriginPointCode(), mtp3ServiceAccessPoint.getOpc());
            assertEquals(serviceAccessConfig.getNetworkIndicator(), mtp3ServiceAccessPoint.getNi());

            if (serviceAccessConfig.getLocalGtDigits().isEmpty()) {
                assertNull(mtp3ServiceAccessPoint.getLocalGtDigits());
            } else {
                assertEquals(serviceAccessConfig.getLocalGtDigits(), mtp3ServiceAccessPoint.getLocalGtDigits());
            }

            var mtp3DestinationConfigStackMap = mtp3ServiceAccessPoint.getMtp3Destinations();

            var mtp3DestinationConfigList = settingsSCCP.getServiceAccessPoints().getMtp3Destinations().stream().filter(
                    s -> s.getSccpSapId() == serviceAccessConfig.getId()).toList();

            assertEquals(mtp3DestinationConfigList.size(), mtp3DestinationConfigStackMap.size());
            //Check values
            mtp3DestinationConfigList.forEach(mtp3DestinationConfig -> {
                var mtp3DestinationConfigStack = mtp3DestinationConfigStackMap.get(mtp3DestinationConfig.getId());
                assertNotNull(mtp3DestinationConfigStack);
                assertEquals(mtp3DestinationConfig.getFirstPointCode(), mtp3DestinationConfigStack.getFirstDpc());
                assertEquals(mtp3DestinationConfig.getLastPointCode(), mtp3DestinationConfigStack.getLastDpc());
                assertEquals(mtp3DestinationConfig.getFirstSls(), mtp3DestinationConfigStack.getFirstSls());
                assertEquals(mtp3DestinationConfig.getLastSls(), mtp3DestinationConfigStack.getLastSls());
                assertEquals(mtp3DestinationConfig.getSlsMask(), mtp3DestinationConfigStack.getSlsMask());
            });
        });
    }

    private void checkSccpAddresses(SccpStack sccpStack, SettingsSCCP settingsSCCP) {
        SccpExtModuleImpl sccpExtModule = (SccpExtModuleImpl) sccpStack.getSs7ExtSccpInterface();
        var routingAddressMap =  sccpExtModule.getRouterExt().getRoutingAddresses();
        settingsSCCP.getAddresses().forEach(addressConfig -> {
            assertTrue(routingAddressMap.containsKey(addressConfig.getId()));
            var routingAddress = routingAddressMap.get(addressConfig.getId());
            assertEquals(addressConfig.getPointCode(), routingAddress.getSignalingPointCode());
            assertEquals(addressConfig.getSubsystemNumber(), routingAddress.getSubsystemNumber());
            assertEquals(addressConfig.getDigits(), routingAddress.getGlobalTitle().getDigits());
        });
    }

    private void checkSccpRules(SccpStack sccpStack, SettingsSCCP settingsSCCP) {
        SccpExtModuleImpl sccpExtModule = (SccpExtModuleImpl) sccpStack.getSs7ExtSccpInterface();
        var rulesMap =  sccpExtModule.getRouterExt().getRules();
        settingsSCCP.getRules().forEach(ruleConfig -> {
            assertTrue(rulesMap.containsKey(ruleConfig.getId()));
            var rule = rulesMap.get(ruleConfig.getId());
            assertEquals(ruleConfig.getId(), rule.getRuleId());
            assertEquals(UtilsEnum.getRuleType(ruleConfig.getRuleTypeId()), rule.getRuleType().getValue());
            assertEquals(UtilsEnum.getLoadSharingAlgorithm(ruleConfig.getLoadSharingAlgorithmId()), rule.getLoadSharingAlgorithm().getValue());
            assertEquals(ruleConfig.getMask(), rule.getMask());
            assertEquals(ruleConfig.getPrimaryAddressId(), rule.getPrimaryAddressId());
            assertEquals(UtilsEnum.getOriginationType(ruleConfig.getOriginationTypeId()), rule.getOriginationType().name());
            if (Objects.isNull(ruleConfig.getSecondaryAddressId())) {
                assertEquals(-1, rule.getSecondaryAddressId());
            } else {
                assertEquals(ruleConfig.getSecondaryAddressId(), rule.getSecondaryAddressId());
            }
            if (Objects.isNull(ruleConfig.getNewCallingPartyAddress())) {
                assertNull(rule.getNewCallingPartyAddressId());
                assertNull(rule.getPatternCallingAddress());
            } else {
                assertEquals(ruleConfig.getNewCallingPartyAddress(), String.valueOf(rule.getNewCallingPartyAddressId()));
                assertEquals(ruleConfig.getCallingPointCode(), rule.getPatternCallingAddress().getSignalingPointCode());
                assertEquals(ruleConfig.getCallingSubsystemNumber(), rule.getPatternCallingAddress().getSubsystemNumber());
                assertEquals(ruleConfig.getCallingGlobalTittleDigits(), rule.getPatternCallingAddress().getGlobalTitle().getDigits());
            }
            assertEquals(ruleConfig.getCalledPointCode(), rule.getPattern().getSignalingPointCode());
            assertEquals(ruleConfig.getCalledSubsystemNumber(), rule.getPattern().getSubsystemNumber());
            assertEquals(ruleConfig.getCalledGlobalTittleDigits(), rule.getPattern().getGlobalTitle().getDigits());
        });
    }
}
