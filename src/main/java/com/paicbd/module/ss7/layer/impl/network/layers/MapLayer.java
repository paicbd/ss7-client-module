package com.paicbd.module.ss7.layer.impl.network.layers;


import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.ss7.layer.impl.network.layers.listeners.MapDialogListener;
import com.paicbd.module.ss7.layer.impl.network.layers.listeners.MapSmsListener;
import com.paicbd.smsc.exception.RTException;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.map.MAPStackImpl;
import org.restcomm.protocols.ss7.map.api.MAPProvider;

@Slf4j
public class MapLayer implements ILayer {

    private final MAPStackImpl map;
    private final String persistDir;
    private final MAPProvider provider;

    //Listeners
    private MapSmsListener mapSmsListener;
    private MapDialogListener mapDialogListener;


    public MAPProvider getMapProvider() {
        return map.getMAPProvider();
    }

    public MAPStackImpl getMapStack() {
        return this.map;
    }


    public MapLayer(String name, TcapLayer tcap, String persistDir) {
        this.map = new MAPStackImpl(name, tcap.getTcapProvider());
        this.provider = this.map.getMAPProvider();
        this.persistDir = persistDir;
    }

    @Override
    public String getName() {
        return map.getName();
    }

    @Override
    public void start() throws RTException {
        log.info("Starting MAP Layer '{}'.", this.getName());
        this.map.setPersistDir(persistDir);
        try {
            this.map.start();
        } catch (Exception e) {
            log.error("Exception when starting MAP Layer '{}'. ", this.getName(), e);
            throw new RTException("Exception when starting MAP Layer", e);
        }
        log.info("MAP Layer '{}' has been started", this.getName());
    }

    @Override
    public void stop() {
        try {
            log.info("Stopping MAP Layer '{}'.", this.getName());
            this.map.stop();
            // remove the listeners
            this.provider.getMAPServiceSms().deactivate();
            this.provider.getMAPServiceSms().removeMAPServiceListener(this.mapSmsListener);
            this.provider.removeMAPDialogListener(this.mapDialogListener);
            log.info("MAP Layer '{}' has been stopped", this.getName());
        } catch (Exception e) {
            log.error("Exception when stopping MAP Layer '{}'. ", this.getName(), e);
        }
    }

    public void setChannelHandler(IChannelHandler channelHandler) {
        log.info("Starting the Map Listeners");
        this.mapSmsListener = new MapSmsListener(channelHandler);
        this.mapDialogListener = new MapDialogListener(channelHandler);
        this.provider.addMAPDialogListener(this.mapDialogListener);
        this.provider.getMAPServiceSms().addMAPServiceListener(this.mapSmsListener);
        this.provider.getMAPServiceSms().activate();
    }
}
