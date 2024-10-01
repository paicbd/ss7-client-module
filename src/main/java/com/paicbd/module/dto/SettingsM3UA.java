package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SettingsM3UA {

    @JsonProperty("general")
    private General general;
    private Associations associations;
    @JsonProperty("application_servers")
    private List<ApplicationServer> applicationServers;
    private List<Route> routes;

    @Getter
    @Setter
    public static class General {
        @JsonProperty("id")
        private int id;
        @JsonProperty("network_id")
        private int networkId;
        @JsonProperty("connect_delay")
        private int connectDelay;
        @JsonProperty("max_sequence_number")
        private Integer maxSequenceNumber;
        @JsonProperty("max_for_route")
        private Integer maxForRoute;
        @JsonProperty("thread_count")
        private Integer threadCount;
        @JsonProperty("routing_label_format")
        private String routingLabelFormat;
        @JsonProperty("heart_beat_time")
        private Integer heartBeatTime;
        @JsonProperty("routing_key_management_enabled")
        private Boolean routingKeyManagementEnabled;
        @JsonProperty("use_lowest_bit_for_link")
        private Boolean useLowestBitForLink;
        @JsonProperty("cc_delay_threshold_1")
        private double ccDelayThreshold1;
        @JsonProperty("cc_delay_threshold_2")
        private double ccDelayThreshold2;
        @JsonProperty("cc_delay_threshold_3")
        private double ccDelayThreshold3;
        @JsonProperty("cc_delay_back_to_normal_threshold_1")
        private double ccDelayBackToNormalThreshold1;
        @JsonProperty("cc_delay_back_to_normal_threshold_2")
        private double ccDelayBackToNormalThreshold2;
        @JsonProperty("cc_delay_back_to_normal_threshold_3")
        private double ccDelayBackToNormalThreshold3;
    }

    @Getter
    @Setter
    public static class Associations {
        private List<Socket> sockets;
        @JsonProperty("associations")
        private List<Association> associationList;


        @Getter
        @Setter
        public static class Association {
            private int id;
            private String name;
            private String state;
            private int enabled;
            private String peer;
            @JsonProperty("peer_port")
            private int peerPort;
            @JsonProperty("m3ua_heartbeat")
            private boolean m3uaHeartbeat;
            @JsonProperty("m3ua_socket_id")
            private int m3uaSocketId;
            @JsonProperty("asp_name")
            private String aspName;
        }

        @Getter
        @Setter
        public static class Socket {
            private int id;
            private String name;
            private String state;
            private int enabled;
            @JsonProperty("socket_type")
            private String socketType;
            @JsonProperty("transport_type")
            private String transportType;
            @JsonProperty("host_address")
            private String hostAddress;
            @JsonProperty("host_port")
            private int hostPort;
            @JsonProperty("extra_address")
            private String extraAddress;
            @JsonProperty("max_concurrent_connections")
            private int maxConcurrentConnections;
            @JsonProperty("ss7_m3ua_id")
            private int ss7M3uaId;
        }
    }

    @Getter
    @Setter
    public static class ApplicationServer {

        private int id;
        private String name;
        private String state;
        private String functionality;
        private String exchange;
        @JsonProperty("routing_context")
        private int routingContext;
        @JsonProperty("network_appearance")
        private int networkAppearance;
        @JsonProperty("traffic_mode_id")
        private int trafficModeId;
        @JsonProperty("minimum_asp_for_loadshare")
        private int minimumAspForLoadshare;
        @JsonProperty("asp_factories")
        private List<Integer> aspFactories;
    }

    @Getter
    @Setter
    public static class Route {
        private int id;
        @JsonProperty("origination_point_code")
        private int originationPointCode;
        @JsonProperty("destination_point_code")
        private int destinationPointCode;
        @JsonProperty("service_indicator")
        private int serviceIndicator;
        @JsonProperty("traffic_mode_id")
        private int trafficModeId;
        @JsonProperty("app_servers")
        private List<Integer> appServers;
    }

}
