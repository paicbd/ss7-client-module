package com.paicbd.module.ss7.layer.impl.channel;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.ss7.layer.impl.MessageUtil;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.ss7.layer.impl.network.layers.M3uaLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SccpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SctpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.TcapLayer;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.CustomNumberingPlanIndicator;
import com.paicbd.module.utils.CustomTypeOfNumber;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.indicator.NatureOfAddress;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.primitives.AddressNature;
import org.restcomm.protocols.ss7.map.api.primitives.AddressString;
import org.restcomm.protocols.ss7.map.api.primitives.NumberingPlan;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberSMImpl;
import org.restcomm.protocols.ss7.map.primitives.AddressStringImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.restcomm.protocols.ss7.sccp.parameter.GlobalTitle;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;
import redis.clients.jedis.JedisCluster;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapChannelTest {

    @Mock
    AppProperties appProperties;

    @Mock
    JedisCluster jedisCluster;

    @Mock
    CdrProcessor cdrProcessor;

    @Mock
    Gateway gateway;

    @Mock
    ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    @InjectMocks
    ExtendedResource extendedResource;

    @InjectMocks
    MapChannel mapChannel;

    Gateway ss7Gateway = GatewayUtil.getGateway(2720, 2721);
    String path;
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;
    SccpLayer sccpLayer;
    TcapLayer tcapLayer;
    MapLayer mapLayer;



    @BeforeEach
    public void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
        mapLayer = (MapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-MAP", Ss7Utils.LayerType.MAP, ss7Gateway, path, tcapLayer);
    }

    @AfterEach
    void tearDown() throws IOException {
        extendedResource.deleteDirectory(new File(path));
    }


    @Test
    void testReceiveMessageFromListener() throws MAPException {
        startLayers();
        assertDoesNotThrow(() ->  mapChannel.channelInitialize(mapLayer));
        mapLayer.setChannelHandler(mapChannel);

        var sendRoutingInfoForSMRequest = new SendRoutingInfoForSMRequestImpl();
        var channelMessageRequest = Ss7Utils.getMessage(sendRoutingInfoForSMRequest.getMessageType().toString());
        channelMessageRequest.setParameter(Constants.MESSAGE, sendRoutingInfoForSMRequest);
        assertDoesNotThrow(() ->  mapChannel.receiveMessageFromListener(channelMessageRequest));

        var sendRoutingInfoForSMResponse = new SendRoutingInfoForSMResponseImpl();
        var channelMessageResponse = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageResponse.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() ->  mapChannel.receiveMessageFromListener(channelMessageResponse));

        var mapDialogSms = getMapDialog();
        ChannelMessage channelMessageErrorComponent = Ss7Utils.getMessage(Constants.ON_ERROR_COMPONENT);
        var dialogErrorComponent = new MAPErrorMessageAbsentSubscriberSMImpl();
        mapDialogSms.sendErrorComponent(10L, dialogErrorComponent);
        channelMessageErrorComponent.setParameter(Constants.DIALOG, mapDialogSms);
        channelMessageErrorComponent.setParameter(Constants.INVOKE_ID, mapDialogSms.getLocalDialogId());
        channelMessageErrorComponent.setParameter(Constants.MAP_ERROR_MESSAGE, dialogErrorComponent);
        assertDoesNotThrow(() ->  mapChannel.receiveMessageFromListener(channelMessageErrorComponent));

        var mapDialogSmsInvokeTimeout = getMapDialog();
        ChannelMessage channelMessageInvokeTimeout = Ss7Utils.getMessage(Constants.ON_INVOKE_TIMEOUT);
        channelMessageInvokeTimeout.setParameter(Constants.DIALOG, mapDialogSmsInvokeTimeout);
        channelMessageInvokeTimeout.setParameter(Constants.INVOKE_ID, mapDialogSmsInvokeTimeout.getLocalDialogId());
        assertDoesNotThrow(() ->  mapChannel.receiveMessageFromListener(channelMessageInvokeTimeout));

        ChannelMessage channelMessageDialogClose = Ss7Utils.getMessage(Constants.ON_DIALOG_CLOSE);
        channelMessageDialogClose.setParameter(Constants.DIALOG, mapDialogSmsInvokeTimeout);
        channelMessageDialogClose.setParameter(Constants.INVOKE_ID, mapDialogSmsInvokeTimeout.getLocalDialogId());
        assertDoesNotThrow(() ->  mapChannel.receiveMessageFromListener(channelMessageDialogClose));

        stopLayers();
    }


    @Test
    void testChannel() {
        startLayers();
        assertDoesNotThrow(() ->  mapChannel.channelInitialize(mapLayer));
        assertDoesNotThrow(() ->  mapChannel.sendMessage(new MessageEvent()));
        stopLayers();
    }

    void startLayers() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();
        tcapLayer.start();
        mapLayer.start();
    }

    void stopLayers() {
        mapLayer.stop();
        tcapLayer.stop();
        sccpLayer.stop();
        m3uaLayer.stop();
        sctpLayer.stop();
    }

    private MAPDialogSms getMapDialog() throws MAPException {
        var message = MessageUtil.getMessageEvent();

        GlobalTitle globalTitleClientSccpAddress = Ss7Utils.getGlobalTitle(message.getGlobalTitleIndicator(), message.getTranslationType(),
                null, org.restcomm.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, message.getGlobalTitle());
        SccpAddress clientSccpAddress = Ss7Utils.convertToSccpAddress(globalTitleClientSccpAddress, 0, message.getSmscSsn());

        GlobalTitle globalTitleServerSccpAddress = Ss7Utils.getGlobalTitle(message.getGlobalTitleIndicator(), message.getTranslationType(),
                null, CustomNumberingPlanIndicator.fromSmsc(message.getDestAddrNpi().byteValue()).getIndicatorValue(),
                CustomTypeOfNumber.fromSmsc(message.getDestAddrTon().byteValue()).getIndicatorValue(), message.getDestinationAddr());
        SccpAddress serverSccpAddress = Ss7Utils.convertToSccpAddress(globalTitleServerSccpAddress, 0, message.getHlrSsn());


        var mapDialogSms = this.mapLayer.getMapProvider().getMAPServiceSms().createNewDialog(MAPApplicationContext
                        .getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.getInstance(message.getMapVersion())),
                clientSccpAddress, null, serverSccpAddress, null);

        AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, message.getGlobalTitle());

        mapDialogSms.addSendRoutingInfoForSMRequest(Ss7Utils.getMsisdn(message), true, serviceCentreAddress, null, false, null, null,
                null, false, null, false, false, null, null);

        return mapDialogSms;
    }


}