package com.paicbd.module.dto;

import com.google.code.beanmatchers.BeanMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class SettingsDTOTest {

    @Test
    void testGettersAndSettersForGateway() {
        assertThat(Gateway.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsM3UA() {
        assertThat(SettingsM3UA.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsM3UA_General() {
        assertThat(SettingsM3UA.General.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsM3UA_Associations() {
        assertThat(SettingsM3UA.Associations.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsM3UA_Associations_Association() {
        assertThat(SettingsM3UA.Associations.Association.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsM3UA_Associations_Socket() {
        assertThat(SettingsM3UA.Associations.Socket.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsM3UA_ApplicationServer() {
        assertThat(SettingsM3UA.ApplicationServer.class, BeanMatchers.hasValidGettersAndSetters());
    }


    @Test
    void testGettersAndSettersForSettingsM3UA_Route() {
        assertThat(SettingsM3UA.Route.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP() {
        assertThat(SettingsSCCP.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_General() {
        assertThat(SettingsSCCP.General.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_AddressConfig() {
        assertThat(SettingsSCCP.AddressConfig.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_RuleConfig() {
        assertThat(SettingsSCCP.RuleConfig.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_RemoteResourceConfig() {
        assertThat(SettingsSCCP.RemoteResourceConfig.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_ServiceAccessPointsConfig() {
        assertThat(SettingsSCCP.ServiceAccessPointsConfig.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_ServiceAccessConfig() {
        assertThat(SettingsSCCP.ServiceAccessConfig.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsSCCP_Mtp3DestinationConfig() {
        assertThat(SettingsSCCP.Mtp3DestinationConfig.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsMAP() {
        assertThat(SettingsMAP.class, BeanMatchers.hasValidGettersAndSetters());
    }

    @Test
    void testGettersAndSettersForSettingsTCAP() {
        assertThat(SettingsTCAP.class, BeanMatchers.hasValidGettersAndSetters());
    }
}
