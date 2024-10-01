package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.dto.SettingsSCCP;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.paicbd.module.ss7.layer.impl.GatewayUtil.getRuleConfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SccpLayerTest {

    @Mock
    AppProperties appProperties;

    @InjectMocks
    ExtendedResource extendedResource;

    String path;
    Gateway ss7Gateway = GatewayUtil.getGateway(2707, 2708);
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;
    SccpLayer sccpLayer;


    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
    }

    @AfterEach
    void tearDown() throws IOException {
        extendedResource.deleteDirectory(new File(path));
    }

    @Test
    void testStartLayer() {
        sctpLayer.start();
        m3uaLayer.start();

        assertNotNull(sccpLayer.getSccpProvider());
        assertNotNull(sccpLayer.getName());
        assertEquals(ss7Gateway.getName() + "-SCCP", sccpLayer.getName());
        assertDoesNotThrow(() -> sccpLayer.start());

        assertDoesNotThrow(() -> sccpLayer.stop());
        assertDoesNotThrow(() -> m3uaLayer.stop());
        assertDoesNotThrow(() -> sctpLayer.stop());
    }

    @Test
    void testStart_withWrongMTP3() {
        var servicesAccessPoints =  new ArrayList<>(ss7Gateway.getSettingsSCCP().getServiceAccessPoints().getServiceAccess());
        SettingsSCCP.ServiceAccessConfig serviceAccessConfig = new SettingsSCCP.ServiceAccessConfig();
        serviceAccessConfig.setId(1);
        serviceAccessConfig.setName("sap1");
        serviceAccessConfig.setOriginPointCode(100);
        serviceAccessConfig.setNetworkIndicator(2);
        serviceAccessConfig.setLocalGtDigits("");
        serviceAccessConfig.setSs7SccpId(1);
        servicesAccessPoints.add(serviceAccessConfig);
        ss7Gateway.getSettingsSCCP().getServiceAccessPoints().setServiceAccess(servicesAccessPoints);
        assertThrows(RTException.class, () -> sccpLayer.start());
    }

    @Test
    void testStart_withEmptyMTP3() {
        var servicesAccessPoints =  new ArrayList<>(ss7Gateway.getSettingsSCCP().getServiceAccessPoints().getServiceAccess());
        SettingsSCCP.ServiceAccessConfig serviceAccessConfig = new SettingsSCCP.ServiceAccessConfig();
        serviceAccessConfig.setId(2);
        serviceAccessConfig.setName("sap1");
        serviceAccessConfig.setOriginPointCode(100);
        serviceAccessConfig.setNetworkIndicator(2);
        serviceAccessConfig.setLocalGtDigits("");
        serviceAccessConfig.setSs7SccpId(1);
        servicesAccessPoints.add(serviceAccessConfig);
        ss7Gateway.getSettingsSCCP().getServiceAccessPoints().setServiceAccess(servicesAccessPoints);
        assertDoesNotThrow(() -> sccpLayer.start());
    }

    @Test
    void testStart_withSameAddress() {
        var addressList = new ArrayList<>(ss7Gateway.getSettingsSCCP().getAddresses());
        var addressConfig = new SettingsSCCP.AddressConfig();
        addressConfig.setId(3);
        addressConfig.setName("100");
        addressConfig.setDigits("*");
        addressConfig.setAddressIndicator(17);
        addressConfig.setPointCode(100);
        addressConfig.setSubsystemNumber(0);
        addressConfig.setGtIndicator("GT0100");
        addressConfig.setTranslationType(0);
        addressConfig.setNumberingPlanId(1);
        addressConfig.setNatureOfAddressId(4);
        addressConfig.setSs7SccpId(1);
        addressList.add(addressConfig);
        ss7Gateway.getSettingsSCCP().setAddresses(addressList);
        assertThrows(RTException.class, () -> sccpLayer.start());
    }

    @Test
    void testStart_withSameRuleId() {
        var ruleList = new ArrayList<>(ss7Gateway.getSettingsSCCP().getRules());
        SettingsSCCP.RuleConfig ruleConfig =  getRuleConfig(3, "100", 3, 3);
        ruleList.add(ruleConfig);
        ss7Gateway.getSettingsSCCP().setRules(ruleList);
        assertThrows(RTException.class, () -> sccpLayer.start());
    }

    @Test
    void testStart_withCallingPartyAddress() {
        List<SettingsSCCP.RuleConfig> ruleConfigs = new ArrayList<>();
        var ruleWithCallingInfo = getRuleConfig(3, "100", 3, 3);
        ruleWithCallingInfo.setSecondaryAddressId(4);
        ruleWithCallingInfo.setCallingGtIndicator("GT0100");
        ruleWithCallingInfo.setCallingAddressIndicator(16);
        ruleWithCallingInfo.setCallingPointCode(0);
        ruleWithCallingInfo.setCallingSubsystemNumber(0);
        ruleWithCallingInfo.setCallingTranslationType(0);
        ruleWithCallingInfo.setCallingNumberingPlanId(1);
        ruleWithCallingInfo.setCallingNatureOfAddressId(4);
        ruleWithCallingInfo.setCallingGlobalTittleDigits("*");
        ruleWithCallingInfo.setNewCallingPartyAddress("888");
        ruleConfigs.add(ruleWithCallingInfo);
        ruleConfigs.add(getRuleConfig(4, "200", 4, 2));
        ss7Gateway.getSettingsSCCP().setRules(ruleConfigs);
        assertDoesNotThrow(() -> sccpLayer.start());
    }
}