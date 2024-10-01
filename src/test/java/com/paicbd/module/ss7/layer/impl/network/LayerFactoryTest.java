package com.paicbd.module.ss7.layer.impl.network;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.ss7.layer.impl.network.layers.M3uaLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SccpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SctpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.TcapLayer;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.exception.RTException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LayerFactoryTest {

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        Constructor<LayerFactory> constructor = LayerFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }


    @Test
    void testCreateLayerInstance() {
        Gateway ss7Gateway = GatewayUtil.getGateway(2701, 2702);
        var sctpLayer = LayerFactory.createLayerInstance("SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, "");
        assertInstanceOf(SctpLayer.class, sctpLayer);
        var m3uaLayer =  LayerFactory.createLayerInstance("M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, "", sctpLayer);
        assertInstanceOf(M3uaLayer.class, m3uaLayer);
        var sccpLayer =  LayerFactory.createLayerInstance("SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, "", m3uaLayer);
        assertInstanceOf(SccpLayer.class, sccpLayer);
        var tcapLayer =  LayerFactory.createLayerInstance("TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, "", sccpLayer);
        assertInstanceOf(TcapLayer.class, tcapLayer);
        var mapLayer =  LayerFactory.createLayerInstance("MAP", Ss7Utils.LayerType.MAP, ss7Gateway, "", tcapLayer);
        assertInstanceOf(MapLayer.class, mapLayer);

        assertThrows(RTException.class, () ->
                LayerFactory.createLayerInstance("TCAP", Ss7Utils.LayerType.TCAP, null, "", sccpLayer));

    }
}