package com.paicbd.module.ss7.layer.impl.network;


import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.ss7.layer.impl.network.layers.M3uaLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SccpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SctpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.TcapLayer;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.exception.RTException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


@Slf4j
public class LayerFactory {

    private LayerFactory() {
        throw new IllegalStateException("Factory Class");
    }

    public static ILayer createLayerInstance(String name, Ss7Utils.LayerType layerType,
                                             Gateway gateway, String persistDir,
                                             ILayer... transportLayerName) throws RTException {
        try {
            return switch (layerType) {
                case SCTP ->
                        new SctpLayer(name, gateway.getSettingsM3UA(), persistDir);
                case M3UA ->
                        new M3uaLayer(name, gateway.getSettingsM3UA(), (SctpLayer) transportLayerName[0], persistDir);
                case SCCP ->
                        new SccpLayer(name, gateway.getSettingsSCCP(), (M3uaLayer) transportLayerName[0], persistDir);
                case TCAP ->
                        new TcapLayer(name, gateway.getSettingsTCAP(), (SccpLayer) transportLayerName[0], persistDir);
                case MAP ->
                        new MapLayer(name, (TcapLayer) transportLayerName[0], persistDir);
            };
        } catch (Exception e) {
            log.error("Caught exception while initializing layer {} ", name, e);
            throw new RTException("Caught exception while initializing layer", e);
        }
    }

}
