package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.SettingsM3UA;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.exception.RTException;
import lombok.extern.slf4j.Slf4j;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class SctpLayer implements ILayer {

    private final NettySctpManagementImpl sctp;
    private final SettingsM3UA settingsM3UA;
    private final String persistDir;

    public SctpLayer(String name, SettingsM3UA settingsM3UA, String persistDir) throws IOException {
        this.sctp = new NettySctpManagementImpl(name);
        this.settingsM3UA = settingsM3UA;
        this.persistDir = persistDir;
    }

    public Management getSctpManagement() {
        return sctp;
    }

    @Override
    public void start() throws RTException {
        log.info("Starting SCTP Layer '{}'.", this.getName());
        try {
            this.loadInitConfig();
            this.loadThresholdConfig();
            this.loadAssociations();
            log.info("SCTP Layer '{}' has been started", this.getName());
        } catch (Exception e) {
            log.error("Exception when starting SCTP Layer '{}'. ", this.getName(), e);
            throw new RTException("Exception when starting SCTP Layer", e);
        }
    }

    @Override
    public void stop() {
        try {
            log.info("Stopping SCTP Layer '{}'.", this.getName());
            this.sctp.stop();
            log.info("SCTP Layer '{}' has been stopped", this.getName());
        } catch (Exception e) {
            log.error("Exception when stopping SCTP Layer '{}'. ", this.getName(), e);
        }
    }


    @Override
    public String getName() {
        return sctp.getName();
    }

    private void loadInitConfig() throws Exception {
        log.info("Persist Dir -> {}", this.persistDir);
        this.sctp.setPersistDir(this.persistDir);
        int threadCount = this.settingsM3UA.getGeneral().getThreadCount();
        this.sctp.setSingleThread(threadCount == 1);
        this.sctp.setWorkerThreads(threadCount);
        this.sctp.setBossGroupThreadCount(threadCount);
        this.sctp.setWorkerGroupThreadCount(threadCount);
        this.sctp.start();
    }

    private void loadThresholdConfig() throws Exception {

        if (this.settingsM3UA.getGeneral().getCcDelayThreshold1() > 0)
            sctp.setCongControl_DelayThreshold_1(this.settingsM3UA.getGeneral().getCcDelayThreshold1());

        if (this.settingsM3UA.getGeneral().getCcDelayThreshold2() > 0)
            sctp.setCongControl_DelayThreshold_2(this.settingsM3UA.getGeneral().getCcDelayThreshold2());

        if (this.settingsM3UA.getGeneral().getCcDelayThreshold3() > 0)
            sctp.setCongControl_DelayThreshold_3(this.settingsM3UA.getGeneral().getCcDelayThreshold3());

        if (this.settingsM3UA.getGeneral().getCcDelayBackToNormalThreshold1() > 0)
            sctp.setCongControl_BackToNormalDelayThreshold_1(this.settingsM3UA.getGeneral().getCcDelayBackToNormalThreshold1());

        if (this.settingsM3UA.getGeneral().getCcDelayBackToNormalThreshold2() > 0)
            sctp.setCongControl_BackToNormalDelayThreshold_2(this.settingsM3UA.getGeneral().getCcDelayBackToNormalThreshold2());

        if (this.settingsM3UA.getGeneral().getCcDelayBackToNormalThreshold3() > 0)
            sctp.setCongControl_BackToNormalDelayThreshold_3(this.settingsM3UA.getGeneral().getCcDelayBackToNormalThreshold3());

        this.sctp.setConnectDelay(this.settingsM3UA.getGeneral().getConnectDelay());
        this.sctp.removeAllResources();
    }

    private void loadAssociations() throws Exception {
        if (this.settingsM3UA.getAssociations() != null) {
            for (SettingsM3UA.Associations.Association association : this.settingsM3UA.getAssociations().getAssociationList()) {
                SettingsM3UA.Associations.Socket socketAssociation = getGatewayM3uaSocket(association.getM3uaSocketId());
                Objects.requireNonNull(socketAssociation);
                var socketType = Ss7Utils.AssociationType.valueOf(socketAssociation.getSocketType().toUpperCase());
                var extraHostAddresses = (Objects.isNull(socketAssociation.getExtraAddress()) ||
                        socketAssociation.getExtraAddress().isEmpty()) ? null : socketAssociation.getExtraAddress().split(",");
                if (socketType == Ss7Utils.AssociationType.CLIENT) {
                    this.addClientAssociation(socketAssociation, association, extraHostAddresses);
                } else {
                    this.addServerAssociation(socketAssociation, association, extraHostAddresses);
                }
            }
        }
    }

    private SettingsM3UA.Associations.Socket getGatewayM3uaSocket(int m3uaSocketId) {
        SettingsM3UA.Associations.Socket socket;
        var optionalSocket = this.settingsM3UA.getAssociations().getSockets().stream().filter(
                s -> s.getId() == m3uaSocketId).findFirst();
        if (optionalSocket.isPresent()) {
            socket = optionalSocket.get();
            return socket;
        }
        log.warn("No socket found for id {} in {}", m3uaSocketId, this.getName());
        return null;
    }

    private List<SettingsM3UA.Associations.Association> getAssociationListBySocket(int m3uaSocketId) {
        return this.settingsM3UA.getAssociations().getAssociationList().stream().filter(association -> association.getM3uaSocketId() == m3uaSocketId).toList();
    }

    private void addClientAssociation(SettingsM3UA.Associations.Socket socket,
                                      SettingsM3UA.Associations.Association association,
                                      String[] extraHostAddresses) throws Exception {

        this.sctp.addAssociation(socket.getHostAddress(), socket.getHostPort(),
                association.getPeer(), association.getPeerPort(),
                association.getName(), IpChannelType.SCTP,
                extraHostAddresses);
    }

    private void addServerAssociation(SettingsM3UA.Associations.Socket socket,
                                      SettingsM3UA.Associations.Association association,
                                      String[] extraHostAddresses) throws Exception {

        var serverList = this.sctp.getServers().isEmpty();

        if (serverList) {
            this.sctp.addServer(socket.getName(), socket.getHostAddress(),
                    socket.getHostPort(), IpChannelType.SCTP,
                    false,
                    socket.getMaxConcurrentConnections(),
                    extraHostAddresses);

            this.sctp.startServer(socket.getName());
        }
        this.sctp.addServerAssociation(association.getPeer(), association.getPeerPort(), socket.getName(),
                association.getName(), IpChannelType.SCTP);

    }

    public void manageAssociation(String assocName, boolean start) {
        try {
            if (start) {
                this.sctp.startAssociation(assocName);
                return;
            }
            this.sctp.stopAssociation(assocName);
        } catch (Exception e) {
            log.error("Error on {} {} association", start ? "starting" : "stopping", assocName, e);
        }
    }

    public void manageSocket(int idSocket, boolean start) {
        try {
            var socket = this.getGatewayM3uaSocket(idSocket);
            if (Objects.isNull(socket)) {
                log.warn("No socket found for id {} in {} for {} socket", idSocket, this.getName(), start ? "start" : "stop");
                return;
            }
            var socketType = Ss7Utils.AssociationType.valueOf(socket.getSocketType().toUpperCase());
            this.changeAssociationsBySocket(socket.getId(), start);
            if (Ss7Utils.AssociationType.SERVER.equals(socketType)) {
                this.manageSctpServer(socket.getName(), start);
            }
        } catch (Exception e) {
            log.error("Error on manage socket, socketId {}, start {}", idSocket, start, e);
        }
    }

    private void changeAssociationsBySocket(int idSocket, boolean start) {
        this.getAssociationListBySocket(idSocket).forEach(association -> {
            int expectedState = start ? 1 : 0;
            if (association.getEnabled() != expectedState) {
                try {
                    association.setEnabled(expectedState);
                    this.manageAssociation(association.getName(), start);
                } catch (Exception e) {
                    log.error("Error on {} {} association", start ? "start" : "stop", association.getName(), e);
                }
            }
        });
    }

    private void manageSctpServer(String socketName, boolean start) throws Exception {
        if (start) {
            this.sctp.startServer(socketName);
        } else {
            this.sctp.stopServer(socketName);
        }
    }
}
