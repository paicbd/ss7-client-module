package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.SettingsSCCP;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.exception.RTException;
import com.paicbd.smsc.utils.UtilsEnum;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.indicator.NumberingPlan;
import org.restcomm.protocols.ss7.sccp.LoadSharingAlgorithm;
import org.restcomm.protocols.ss7.sccp.OriginationType;
import org.restcomm.protocols.ss7.sccp.RuleType;
import org.restcomm.protocols.ss7.sccp.SccpProvider;
import org.restcomm.protocols.ss7.sccp.impl.SccpStackImpl;
import org.restcomm.protocols.ss7.sccp.impl.parameter.BCDOddEncodingScheme;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;
import org.restcomm.protocols.ss7.sccpext.impl.SccpExtModuleImpl;
import org.restcomm.protocols.ss7.ss7ext.Ss7ExtInterface;
import org.restcomm.protocols.ss7.ss7ext.Ss7ExtInterfaceImpl;

import java.util.List;

@Slf4j
public class SccpLayer implements ILayer {

    private final SccpStackImpl sccp;
    private final SccpExtModuleImpl sccpExtModule;
    private final SettingsSCCP settingsSCCP;
    private final String persistDir;


    public SccpProvider getSccpProvider() {
        return sccp.getSccpProvider();
    }

    public SccpLayer(String name, SettingsSCCP settingsSCCP, M3uaLayer m3ua, String persistDir) {
        this.settingsSCCP = settingsSCCP;
        this.persistDir = persistDir;
        Ss7ExtInterface ss7ExtInterface = new Ss7ExtInterfaceImpl();
        this.sccpExtModule = new SccpExtModuleImpl();
        ss7ExtInterface.setSs7ExtSccpInterface(this.sccpExtModule);
        this.sccp = new SccpStackImpl(name, ss7ExtInterface);
        this.sccp.setMtp3UserPart(this.settingsSCCP.getGeneral().getId(), m3ua.getMtp3UserPart());
    }

    @Override
    public String getName() {
        return sccp.getName();
    }

    @Override
    public void start() throws RTException {
        try {
            log.info("Starting SCCP Layer '{}'.", this.getName());
            this.sccp.setPersistDir(persistDir);
            this.sccp.start();
            this.sccp.removeAllResources();

            this.loadRemoteResources();
            this.loadServiceAccessPoints();
            this.loadAddresses();
            this.loadRules();
            log.info("SCCP Layer '{}' has been started", this.getName());
        } catch (Exception e) {
            log.error("Exception when starting SCCP Layer '{}'. ", this.getName(), e);
            throw new RTException("Exception when starting SCCP Layer", e);
        }
    }

    @Override
    public void stop() {
        try {
            log.info("Stopping SCCP Layer '{}'.", this.getName());
            this.sccp.removeAllResources();
            this.sccp.stop();
            log.info("SCCP Layer '{}' has been stopped", this.getName());
        } catch (Exception e) {
            log.error("Exception when stopping SCCP Layer '{}'. ", this.getName(), e);
        }
    }

    private void loadRemoteResources() throws Exception {
        List<Integer> remoteSpcList = this.settingsSCCP.getRemoteResources().stream()
                .map(SettingsSCCP.RemoteResourceConfig::getRemoteSpc)
                .distinct()
                .toList();

        int remoteSpcId = 0;
        for (int remoteSpc : remoteSpcList) {
            remoteSpcId++;
            this.sccp.getSccpResource().addRemoteSpc(remoteSpcId, remoteSpc, 0, 0);
        }

        int remoteSsnId = 0;
        for (var remoteResourceConfig : this.settingsSCCP.getRemoteResources()) {
            remoteSsnId++;
            this.sccp.getSccpResource().addRemoteSsn(remoteSsnId, remoteResourceConfig.getRemoteSpc(),
                    remoteResourceConfig.getRemoteSsn(), 0, remoteResourceConfig.isMarkProhibited());
        }
    }

    private void loadServiceAccessPoints() {
        this.settingsSCCP.getServiceAccessPoints().getServiceAccess().forEach(serviceAccessConfig -> {
            try {
                var mtp3DestinationConfig = getMtp3DestinationConfig(serviceAccessConfig.getId());
                sccp.getRouter().addMtp3ServiceAccessPoint(
                        serviceAccessConfig.getId(),
                        this.settingsSCCP.getGeneral().getId(),
                        serviceAccessConfig.getOriginPointCode(),
                        serviceAccessConfig.getNetworkIndicator(),
                        0, serviceAccessConfig.getLocalGtDigits());
                // add the Mtp3Destinations

                for (var mtp3Destination : mtp3DestinationConfig) {
                    sccp.getRouter().addMtp3Destination(
                            serviceAccessConfig.getId(),
                            mtp3Destination.getId(),
                            mtp3Destination.getFirstPointCode(),
                            mtp3Destination.getLastPointCode(),
                            mtp3Destination.getFirstSls(),
                            mtp3Destination.getLastSls(),
                            mtp3Destination.getSlsMask());
                }

            } catch (Exception ex) {
                throw new RTException("Error on load Service Access Points for " + this.getName(), ex);
            }
        });
    }

    private void loadAddresses() {
        this.settingsSCCP.getAddresses().forEach(addressConfig -> {
            try {
                GlobalTitle globalTitle = Ss7Utils.getGlobalTitle(
                        addressConfig.getGtIndicator(),
                        addressConfig.getTranslationType(),
                        new BCDOddEncodingScheme(),
                        NumberingPlan.valueOf(addressConfig.getNumberingPlanId()),
                        NatureOfAddress.valueOf(addressConfig.getNatureOfAddressId()),
                        addressConfig.getDigits()
                );

                SccpAddress sccpAddress = Ss7Utils.convertToSccpAddress(
                        globalTitle,
                        addressConfig.getPointCode(),
                        addressConfig.getSubsystemNumber()
                );
                sccpExtModule.getRouterExt().addRoutingAddress(addressConfig.getId(), sccpAddress);
            } catch (Exception e) {
                throw new RTException("Error on load address for " + this.getName(), e);
            }
        });
    }

    private void loadRules() {
        this.settingsSCCP.getRules().forEach(ruleConfig -> {
            GlobalTitle globalTitlePatternCalledAddress = Ss7Utils.getGlobalTitle(
                    ruleConfig.getCalledGtIndicator(),
                    ruleConfig.getCalledTranslationType(),
                    new BCDOddEncodingScheme(),
                    NumberingPlan.valueOf(ruleConfig.getCalledNumberingPlanId()),
                    NatureOfAddress.valueOf(ruleConfig.getCalledNatureOfAddressId()),
                    ruleConfig.getCalledGlobalTittleDigits()
            );

            SccpAddress sccpAddressPatternCalledAddress = Ss7Utils.convertToSccpAddress(globalTitlePatternCalledAddress, ruleConfig.getCalledPointCode(), ruleConfig.getCalledSubsystemNumber());

            SccpAddress sccpAddressPatternCallingAddress = null;
            if (ruleConfig.getCallingGtIndicator() != null) {
                GlobalTitle globalTitlePatternCallingAddress = Ss7Utils.getGlobalTitle(
                        ruleConfig.getCallingGtIndicator(),
                        ruleConfig.getCallingTranslationType(),
                        new BCDOddEncodingScheme(),
                        NumberingPlan.valueOf(ruleConfig.getCallingNumberingPlanId()),
                        NatureOfAddress.valueOf(ruleConfig.getCallingNatureOfAddressId()),
                        ruleConfig.getCallingGlobalTittleDigits()
                );


                sccpAddressPatternCallingAddress = Ss7Utils.convertToSccpAddress(globalTitlePatternCallingAddress, ruleConfig.getCallingPointCode(), ruleConfig.getCallingSubsystemNumber());
            }

            try {
                sccpExtModule.getRouterExt().addRule(
                        ruleConfig.getId(),
                        RuleType.getInstance(UtilsEnum.getRuleType(ruleConfig.getRuleTypeId())),
                        LoadSharingAlgorithm.valueOf(UtilsEnum.getLoadSharingAlgorithm(ruleConfig.getLoadSharingAlgorithmId())),
                        OriginationType.valueOf(UtilsEnum.getOriginationType(ruleConfig.getOriginationTypeId())),
                        sccpAddressPatternCalledAddress,
                        ruleConfig.getMask().toUpperCase(),
                        ruleConfig.getPrimaryAddressId() == null ? -1 : ruleConfig.getPrimaryAddressId(),
                        ruleConfig.getSecondaryAddressId() == null ? -1 : ruleConfig.getSecondaryAddressId(),
                        (ruleConfig.getNewCallingPartyAddress() != null) ? Integer.parseInt(ruleConfig.getNewCallingPartyAddress()) : null,
                        0,
                        sccpAddressPatternCallingAddress);
            } catch (Exception e) {
                throw new RTException("Error on load rules for " + this.getName(), e);
            }
        });
    }

    private List<SettingsSCCP.Mtp3DestinationConfig> getMtp3DestinationConfig(int id) {
        var mtp3DestinationConfigList = this.settingsSCCP.getServiceAccessPoints().getMtp3Destinations().stream().filter(
                s -> s.getSccpSapId() == id).toList();
        if (mtp3DestinationConfigList.isEmpty()) {
            log.warn("No Mtp3DestinationConfig found for id {}", id);
            return Collections.emptyList();
        }
        return mtp3DestinationConfigList;
    }

}
