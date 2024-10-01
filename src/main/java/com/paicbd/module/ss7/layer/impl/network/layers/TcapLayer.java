package com.paicbd.module.ss7.layer.impl.network.layers;

import com.paicbd.module.dto.SettingsTCAP;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.smsc.exception.RTException;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.tcap.TCAPStackImpl;
import org.restcomm.protocols.ss7.tcap.api.TCAPProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class TcapLayer implements ILayer {

    private final TCAPStackImpl tcap;
    private final String persistDir;
    private final SettingsTCAP settingsTCAP;
    private final List<Integer> ssnList;

    public TcapLayer(String name, SettingsTCAP settingsTCAP, SccpLayer sccp, String persistDir) {
        this.ssnList = new ArrayList<>(Arrays.stream(settingsTCAP.getSsnList().split(","))
                .map(Integer::parseInt)
                .toList());
        int firstSsn = ssnList.removeFirst();
        this.tcap = new TCAPStackImpl(name, sccp.getSccpProvider(), firstSsn);
        this.persistDir = persistDir;
        this.settingsTCAP = settingsTCAP;
    }

    public TCAPProvider getTcapProvider() {
        return tcap.getProvider();
    }

    @Override
    public String getName() {
        return this.tcap.getName();
    }

    @Override
    public void start() throws RTException {
        log.info("Starting TCAP Layer '{}'.", this.getName());
        try {
            if (!this.ssnList.isEmpty()) {
                this.tcap.setExtraSsns(this.ssnList);
            }
            this.tcap.setPersistDir(this.persistDir);
            this.tcap.setPreviewMode(this.settingsTCAP.isPreviewMode());
            this.tcap.start();
            this.tcap.setDialogIdRangeStart(this.settingsTCAP.getDialogIdRangeStart());
            this.tcap.setDialogIdRangeEnd(this.settingsTCAP.getDialogIdRangeEnd());
            this.tcap.setDoNotSendProtocolVersion(this.settingsTCAP.isDoNotSendProtocolVersion());
            this.tcap.setDialogIdleTimeout(this.settingsTCAP.getDialogIdleTimeout());
            this.tcap.setInvokeTimeout(this.settingsTCAP.getInvokeTimeout());
            this.tcap.setMaxDialogs(this.settingsTCAP.getMaxDialogs());
            this.tcap.setSwapTcapIdBytes(this.settingsTCAP.isSwapTcapIdEnabled());
            this.tcap.setSlsRange(this.settingsTCAP.getSlsRangeId());
            this.tcap.setCongControl_ExecutorDelayThreshold_1(this.settingsTCAP.getExrDelayThr1());
            this.tcap.setCongControl_ExecutorDelayThreshold_2(this.settingsTCAP.getExrDelayThr2());
            this.tcap.setCongControl_ExecutorDelayThreshold_3(this.settingsTCAP.getExrDelayThr3());
            this.tcap.setCongControl_ExecutorBackToNormalDelayThreshold_1(this.settingsTCAP.getExrBackToNormalDelayThr1());
            this.tcap.setCongControl_ExecutorBackToNormalDelayThreshold_2(this.settingsTCAP.getExrBackToNormalDelayThr2());
            this.tcap.setCongControl_ExecutorBackToNormalDelayThreshold_3(this.settingsTCAP.getExrBackToNormalDelayThr3());
            this.tcap.setCongControl_MemoryThreshold_1(this.settingsTCAP.getMemoryMonitorThr1());
            this.tcap.setCongControl_MemoryThreshold_2(this.settingsTCAP.getMemoryMonitorThr2());
            this.tcap.setCongControl_MemoryThreshold_3(this.settingsTCAP.getMemoryMonitorThr3());
            this.tcap.setCongControl_BackToNormalMemoryThreshold_1(this.settingsTCAP.getMemBackToNormalDelayThr1());
            this.tcap.setCongControl_BackToNormalMemoryThreshold_2(this.settingsTCAP.getMemBackToNormalDelayThr2());
            this.tcap.setCongControl_BackToNormalMemoryThreshold_3(this.settingsTCAP.getMemBackToNormalDelayThr3());
            this.tcap.setCongControl_blockingIncomingTcapMessages(this.settingsTCAP.isBlockingIncomingTcapMessages());
            log.info("TCAP Layer '{}' has been started", this.getName());
        } catch (Exception e) {
            log.error("Exception when starting TCAP Layer '{}'. ", this.getName(), e);
            throw new RTException("Exception when starting TCAP Layer", e);
        }

    }

    @Override
    public void stop() {
        log.info("Stopping TCAP Layer '{}'.", this.getName());
        this.tcap.stop();
        log.info("TCAP Layer '{}' has been stopped", this.getName());
    }

}
