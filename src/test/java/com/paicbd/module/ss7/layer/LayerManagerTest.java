package com.paicbd.module.ss7.layer;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayerManagerTest {

    @Mock
    JedisCluster jedisCluster;

    @Mock
    AppProperties appProperties;

    @Mock
    CdrProcessor cdrProcessor;

    @Mock
    ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    @InjectMocks
    ExtendedResource extendedResource;

    LayerManager layerManager;

    Gateway gateway = GatewayUtil.getGateway(2040, 2042);

    private void initLayerManager() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        String persistDirectoryPath = extendedResource.createDirectory(gateway.getName());
        layerManager = new LayerManager(gateway, persistDirectoryPath, jedisCluster, cdrProcessor, appProperties, errorCodeMappingConcurrentHashMap);
    }

    @Test
    void testFetchAllItemsEmpty() throws Exception {
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        when(appProperties.getTpsPerGw()).thenReturn(10);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(jedisCluster.llen(anyString())).thenReturn(100L);
        initLayerManager();
        StepVerifier.create(invokeFetchAllItems(layerManager)).expectSubscription().expectNextCount(0).verifyComplete();
    }

    @Test
    void testFetchAllItems() throws Exception {
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        when(appProperties.getTpsPerGw()).thenReturn(10);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(jedisCluster.llen(anyString())).thenReturn(100L);
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(getMessages());
        initLayerManager();
        StepVerifier.create(invokeFetchAllItems(layerManager)).expectSubscription().expectNextCount(1).verifyComplete();
    }


    @SuppressWarnings("unchecked")
    private Flux<List<MessageEvent>> invokeFetchAllItems(LayerManager layerManager) throws Exception {
        Class<?> clazz = layerManager.getClass();
        Method method = clazz.getDeclaredMethod("fetchAllItems");
        method.setAccessible(true);
        return  (Flux<List<MessageEvent>>) method.invoke(layerManager);
    }

    @SuppressWarnings("unchecked")
    private Flux<Void> invokeProcessor(LayerManager layerManager) throws Exception {
        Class<?> clazz = layerManager.getClass();
        Method method = clazz.getDeclaredMethod("processor");
        method.setAccessible(true);
        return  (Flux<Void>) method.invoke(layerManager);
    }


    @Test
    void testStopLayerManager() throws IOException {
        initLayerManager();
        assertDoesNotThrow(() -> layerManager.connect());
        assertDoesNotThrow(() -> layerManager.stopLayerManager());
    }

    @Test
    void testGetPersistDirectory() throws IOException {
        initLayerManager();
        assertDoesNotThrow(() -> layerManager.getPersistDirectory());
        assertNotNull(layerManager.getPersistDirectory());
    }

    private List<String> getMessages() {
        return List.of(
                "{\"msisdn\":null,\"id\":\"1722442489770-7788608933795\",\"message_id\":\"1722442489766-7788604799226\",\"system_id\":\"smppsp\",\"deliver_sm_id\":null,\"deliver_sm_server_id\":null,\"command_status\":0,\"sequence_number\":2,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"6666\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"5555\",\"esm_class\":3,\"validity_period\":\"60\",\"registered_delivery\":1,\"data_coding\":0,\"sm_default_msg_id\":0,\"short_message\":\"Hello!\",\"delivery_receipt\":null,\"status\":null,\"error_code\":null,\"check_submit_sm_response\":null,\"optional_parameters\":null,\"origin_network_type\":\"SP\",\"origin_protocol\":\"SMPP\",\"origin_network_id\":3,\"dest_network_type\":\"GW\",\"dest_protocol\":\"SMPP\",\"dest_network_id\":1,\"routing_id\":1,\"address_nature_msisdn\":null,\"numbering_plan_msisdn\":null,\"remote_dialog_id\":null,\"local_dialog_id\":null,\"sccp_called_party_address_pc\":null,\"sccp_called_party_address_ssn\":null,\"sccp_called_party_address\":null,\"sccp_calling_party_address_pc\":null,\"sccp_calling_party_address_ssn\":null,\"sccp_calling_party_address\":null,\"global_title\":null,\"global_title_indicator\":null,\"translation_type\":null,\"smsc_ssn\":null,\"hlr_ssn\":null,\"msc_ssn\":null,\"map_version\":null,\"is_retry\":false,\"retry_dest_network_id\":\"\",\"retry_number\":null,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":0,\"accumulated_time\":0,\"drop_map_sri\":false,\"network_id_to_map_sri\":-1,\"network_id_to_permanent_failure\":-1,\"drop_temp_failure\":false,\"network_id_temp_failure\":-1,\"imsi\":null,\"network_node_number\":null,\"network_node_number_nature_of_address\":null,\"network_node_number_numbering_plan\":null,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":null,\"total_segment\":null,\"segment_sequence\":null,\"originator_sccp_address\":null,\"udhi\":\"0\",\"udh_json\":null,\"parent_id\":null,\"is_dlr\":false,\"message_parts\":null}",
                "{\"msisdn\":null,\"id\":\"1722442615535-7914373631079\",\"message_id\":\"1722442615535-7914373573310\",\"system_id\":\"smppsp\",\"deliver_sm_id\":null,\"deliver_sm_server_id\":null,\"command_status\":0,\"sequence_number\":4,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"6666\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"5555\",\"esm_class\":3,\"validity_period\":\"60\",\"registered_delivery\":1,\"data_coding\":0,\"sm_default_msg_id\":0,\"short_message\":\"Hello!\",\"delivery_receipt\":null,\"status\":null,\"error_code\":null,\"check_submit_sm_response\":null,\"optional_parameters\":null,\"origin_network_type\":\"SP\",\"origin_protocol\":\"SMPP\",\"origin_network_id\":3,\"dest_network_type\":\"GW\",\"dest_protocol\":\"HTTP\",\"dest_network_id\":3,\"routing_id\":1,\"address_nature_msisdn\":null,\"numbering_plan_msisdn\":null,\"remote_dialog_id\":null,\"local_dialog_id\":null,\"sccp_called_party_address_pc\":null,\"sccp_called_party_address_ssn\":null,\"sccp_called_party_address\":null,\"sccp_calling_party_address_pc\":null,\"sccp_calling_party_address_ssn\":null,\"sccp_calling_party_address\":null,\"global_title\":null,\"global_title_indicator\":null,\"translation_type\":null,\"smsc_ssn\":null,\"hlr_ssn\":null,\"msc_ssn\":null,\"map_version\":null,\"is_retry\":false,\"retry_dest_network_id\":\"\",\"retry_number\":null,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":0,\"accumulated_time\":0,\"drop_map_sri\":false,\"network_id_to_map_sri\":-1,\"network_id_to_permanent_failure\":-1,\"drop_temp_failure\":false,\"network_id_temp_failure\":-1,\"imsi\":null,\"network_node_number\":null,\"network_node_number_nature_of_address\":null,\"network_node_number_numbering_plan\":null,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":null,\"total_segment\":null,\"segment_sequence\":null,\"originator_sccp_address\":null,\"udhi\":\"0\",\"udh_json\":null,\"parent_id\":null,\"is_dlr\":false,\"message_parts\":null}",
                "{\"msisdn\":null,\"id\":\"1722446896082-12194920127675\",\"message_id\":\"1722446896081-12194920043917\",\"system_id\":\"smppsp\",\"deliver_sm_id\":null,\"deliver_sm_server_id\":null,\"command_status\":0,\"sequence_number\":5,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"6666\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"5555\",\"esm_class\":3,\"validity_period\":\"60\",\"registered_delivery\":1,\"data_coding\":0,\"sm_default_msg_id\":0,\"short_message\":\"Hello!\",\"delivery_receipt\":null,\"status\":null,\"error_code\":null,\"check_submit_sm_response\":null,\"optional_parameters\":null,\"origin_network_type\":\"SP\",\"origin_protocol\":\"SMPP\",\"origin_network_id\":3,\"dest_network_type\":\"GW\",\"dest_protocol\":\"SS7\",\"dest_network_id\":5,\"routing_id\":1,\"address_nature_msisdn\":null,\"numbering_plan_msisdn\":null,\"remote_dialog_id\":null,\"local_dialog_id\":null,\"sccp_called_party_address_pc\":null,\"sccp_called_party_address_ssn\":null,\"sccp_called_party_address\":null,\"sccp_calling_party_address_pc\":null,\"sccp_calling_party_address_ssn\":null,\"sccp_calling_party_address\":null,\"global_title\":null,\"global_title_indicator\":null,\"translation_type\":null,\"smsc_ssn\":null,\"hlr_ssn\":null,\"msc_ssn\":null,\"map_version\":null,\"is_retry\":false,\"retry_dest_network_id\":\"\",\"retry_number\":null,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":0,\"accumulated_time\":0,\"drop_map_sri\":false,\"network_id_to_map_sri\":-1,\"network_id_to_permanent_failure\":-1,\"drop_temp_failure\":false,\"network_id_temp_failure\":-1,\"imsi\":null,\"network_node_number\":null,\"network_node_number_nature_of_address\":null,\"network_node_number_numbering_plan\":null,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":null,\"total_segment\":null,\"segment_sequence\":null,\"originator_sccp_address\":null,\"udhi\":\"0\",\"udh_json\":null,\"parent_id\":null,\"is_dlr\":false,\"message_parts\":null}",
                "{\"msisdn\":null,\"id\":\"1722446896082-12194920127675\",\"message_id\":\"1722446896081-12194920043917\",\"system_id\":\"smppsp\",\"deliver_sm_id\":null,\"deliver_sm_server_id\":null,\"command_status\":0,\"sequence_number\":5,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"6666\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"5555\",\"esm_class\":3,\"validity_period\":\"60\",\"registered_delivery\":1,\"data_coding\":0,\"sm_default_msg_id\":0,\"short_message\":\"Hello!\",\"delivery_receipt\":null,\"status\":null,\"error_code\":null,\"check_submit_sm_response\":null,\"optional_parameters\":null,\"origin_network_type\":\"SP\",\"origin_protocol\":\"SMPP\",\"origin_network_id\":3,\"dest_network_type\":\"GW\",\"dest_protocol\":\"SS7\",\"dest_network_id\":5,\"routing_id\":1,\"address_nature_msisdn\":null,\"numbering_plan_msisdn\":null,\"remote_dialog_id\":null,\"local_dialog_id\":null,\"sccp_called_party_address_pc\":null,\"sccp_called_party_address_ssn\":null,\"sccp_called_party_address\":null,\"sccp_calling_party_address_pc\":null,\"sccp_calling_party_address_ssn\":null,\"sccp_calling_party_address\":null,\"global_title\":null,\"global_title_indicator\":null,\"translation_type\":null,\"smsc_ssn\":null,\"hlr_ssn\":null,\"msc_ssn\":null,\"map_version\":null,\"is_retry\":false,\"retry_dest_network_id\":\"\",\"retry_number\":null,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":0,\"accumulated_time\":0,\"drop_map_sri\":false,\"network_id_to_map_sri\":-1,\"network_id_to_permanent_failure\":-1,\"drop_temp_failure\":false,\"network_id_temp_failure\":-1,\"imsi\":null,\"network_node_number\":null,\"network_node_number_nature_of_address\":null,\"network_node_number_numbering_plan\":null,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":null,\"total_segment\":null,\"segment_sequence\":null,\"originator_sccp_address\":null,\"udhi\":\"0\",\"udh_json\":null,\"parent_id\":null,\"is_dlr\":true,\"message_parts\":null}"
        );
    }
}