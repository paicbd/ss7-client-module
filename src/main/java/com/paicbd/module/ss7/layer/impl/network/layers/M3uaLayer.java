package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.SettingsM3UA;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.smsc.exception.RTException;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.m3ua.ExchangeType;
import org.restcomm.protocols.ss7.m3ua.Functionality;
import org.restcomm.protocols.ss7.m3ua.IPSPType;
import org.restcomm.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.restcomm.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl;
import org.restcomm.protocols.ss7.m3ua.parameter.ParameterFactory;
import org.restcomm.protocols.ss7.mtp.Mtp3UserPart;
import org.restcomm.protocols.ss7.mtp.RoutingLabelFormat;
import org.restcomm.protocols.ss7.ss7ext.Ss7ExtInterfaceImpl;

import java.util.Objects;


@Slf4j
public class M3uaLayer implements ILayer {

    private final M3UAManagementImpl m3ua;
    private final SctpLayer sctp;
    private final String persistDir;
    private final SettingsM3UA settingsM3UA;
    ParameterFactory parameterFactory = new ParameterFactoryImpl();

    public Mtp3UserPart getMtp3UserPart() {
        return m3ua;
    }

    public M3uaLayer(String name, SettingsM3UA settingsM3UA, SctpLayer sctp, String persistDir) {
        this.sctp = sctp;
        this.m3ua = new M3UAManagementImpl(name, "SMSC-SS7", new Ss7ExtInterfaceImpl());
        this.persistDir = persistDir;
        this.settingsM3UA = settingsM3UA;
    }

    @Override
    public void start() throws RTException {
        log.info("Starting M3UA Layer '{}'.", this.getName());
        try {
            this.loadInitConfig();
            this.loadApplicationServers();
            this.loadRoutes();
            log.info("M3UA Layer '{}' has been started", this.getName());
        } catch (Exception e) {
            log.error("Exception when starting M3UA Layer '{}'. ", this.getName(), e);
            throw new RTException("Exception when starting M3UA Layer", e);
        }
    }

    @Override
    public void stop() {
        try {
            log.info("Stopping M3UA Layer '{}'.", this.getName());
            this.m3ua.stop();
            log.info("M3UA Layer '{}' has been stopped", this.getName());
        } catch (Exception e) {
            log.error("Exception when stopping M3UA Layer '{}'. ", this.getName(), e);
        }
    }

    @Override
    public String getName() {
        return m3ua.getName();
    }

    private void loadInitConfig() throws Exception {
        log.info("Persist Dir -> {}", this.persistDir);
        this.m3ua.setPersistDir(this.persistDir);
        this.m3ua.setTransportManagement(this.sctp.getSctpManagement());

        if (this.settingsM3UA.getGeneral().getThreadCount() > 0)
            this.m3ua.setDeliveryMessageThreadCount(this.settingsM3UA.getGeneral().getThreadCount());

        if (this.settingsM3UA.getGeneral().getMaxSequenceNumber() != null && this.settingsM3UA.getGeneral().getMaxSequenceNumber() > 0)
            this.m3ua.setMaxSequenceNumber(this.settingsM3UA.getGeneral().getMaxSequenceNumber());

        if (this.settingsM3UA.getGeneral().getMaxForRoute() != null && this.settingsM3UA.getGeneral().getMaxForRoute() > 0)
            this.m3ua.setMaxAsForRoute(this.settingsM3UA.getGeneral().getMaxForRoute());

        if (this.settingsM3UA.getGeneral().getRoutingLabelFormat() != null)
            this.m3ua.setRoutingLabelFormat(RoutingLabelFormat.getInstance(this.settingsM3UA.getGeneral().getRoutingLabelFormat().toUpperCase()));

        if (this.settingsM3UA.getGeneral().getRoutingKeyManagementEnabled() != null)
            this.m3ua.setRoutingKeyManagementEnabled(this.settingsM3UA.getGeneral().getRoutingKeyManagementEnabled());

        this.m3ua.start();

        if (this.settingsM3UA.getGeneral().getUseLowestBitForLink() != null)
            this.m3ua.setUseLsbForLinksetSelection(this.settingsM3UA.getGeneral().getUseLowestBitForLink());

        this.m3ua.setHeartbeatTime(this.settingsM3UA.getGeneral().getHeartBeatTime());
        this.m3ua.removeAllResources();
    }

    private void loadApplicationServers() throws RTException {
        this.settingsM3UA.getApplicationServers().forEach(as -> {
            String functionality = as.getFunctionality().split("-")[0];
            String ipsType = "";
            if (as.getFunctionality().split("-").length > 1) {
                ipsType = as.getFunctionality().split("-")[1];
            }

            try {
                this.m3ua.createAs(
                        as.getName(),
                        Functionality.getFunctionality(functionality),
                        ExchangeType.getExchangeType(as.getExchange()),
                        IPSPType.getIPSPType(ipsType),
                        this.parameterFactory.createRoutingContext(new long[]{as.getRoutingContext()}),
                        this.parameterFactory.createTrafficModeType(as.getTrafficModeId()),
                        as.getMinimumAspForLoadshare(),
                        this.parameterFactory.createNetworkAppearance(as.getNetworkAppearance())
                );
            } catch (Exception e) {
                throw new RTException("Error on create AS for " + this.getName(), e);
            }

            this.loadAspFactory(as);
        });
    }

    private void loadAspFactory(SettingsM3UA.ApplicationServer applicationServer) {
        applicationServer.getAspFactories().forEach(asp -> {
            try {
                SettingsM3UA.Associations.Association association = this.getGatewayM3uaAssociation(asp);
                Objects.requireNonNull(association);
                this.m3ua.createAspFactory(association.getAspName(),
                        association.getName(),
                        association.isM3uaHeartbeat());

                this.m3ua.assignAspToAs(applicationServer.getName(),
                        association.getAspName());

                this.m3ua.startAsp(association.getAspName());
            } catch (Exception e) {
                throw new RTException("Error on create ASP Factory for " + this.getName(), e);
            }
        });
    }

    private void loadRoutes() {
        this.settingsM3UA.getRoutes().forEach(route -> route.getAppServers().forEach(routeAppServer -> {
            try {
                SettingsM3UA.ApplicationServer applicationServer = getGatewayM3uaApplicationServer(routeAppServer);
                Objects.requireNonNull(applicationServer);
                this.m3ua.addRoute(route.getDestinationPointCode(),
                        route.getOriginationPointCode(),
                        route.getServiceIndicator(), applicationServer.getName());
            } catch (Exception e) {
                throw new RTException("Error on load the route " + route.getId() + " for " + this.getName(), e);
            }
        }));
    }


    private SettingsM3UA.Associations.Association getGatewayM3uaAssociation(int id) {
        var optionalAssociation = this.settingsM3UA.getAssociations().getAssociationList().stream().filter(
                assoc -> assoc.getId() == id).findFirst();

        if (optionalAssociation.isPresent()) {
            return optionalAssociation.get();
        }
        log.warn("No association found for id {}", id);
        return null;
    }

    private SettingsM3UA.ApplicationServer getGatewayM3uaApplicationServer(int id) {
        var optionalApplicationServer = this.settingsM3UA.getApplicationServers().stream().filter(
                as -> as.getId() == id).findFirst();

        if (optionalApplicationServer.isPresent()) {
            return optionalApplicationServer.get();
        }
        log.warn("No application server found for id {} ", id);
        return null;
    }
}
