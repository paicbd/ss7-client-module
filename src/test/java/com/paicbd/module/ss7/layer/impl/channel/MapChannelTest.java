package com.paicbd.module.ss7.layer.impl.channel;

import com.paicbd.module.ss7.MessageProcessing;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.Ss7Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContext;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberSMImpl;
import org.restcomm.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapChannelTest {

    @Mock
    MessageProcessing messageProcessing;

    @Mock
    MAPDialogSms mapDialogSms;

    @InjectMocks
    MapChannel mapChannel;


    @Test
    @DisplayName("Receive message from listener when channel message is request then process message")
    void receiveMessageFromListenerWhenChannelMessageIsRequestThenProcessMessage()  {
        var moForwardShortMessageRequest = new MoForwardShortMessageRequestImpl();
        var channelMessageRequest = Ss7Utils.createChannelMessage(moForwardShortMessageRequest.getMessageType().toString());
        channelMessageRequest.setParameter(Constants.MESSAGE, moForwardShortMessageRequest);
        mapChannel.receiveMessageFromListener(channelMessageRequest);
        verify(messageProcessing).processMessage(channelMessageRequest);
    }

    @Test
    @DisplayName("Receive message from listener when channel message is response then process message")
    void receiveMessageFromListenerWhenChannelMessageIsResponseThenProcessMessage()  {
        var sendRoutingInfoForSMResponse = new SendRoutingInfoForSMResponseImpl();
        var channelMessageResponse = Ss7Utils.createChannelMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageResponse.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        mapChannel.receiveMessageFromListener(channelMessageResponse);
        verify(messageProcessing).processMessage(channelMessageResponse);
    }

    @Test
    @DisplayName("Receive message from listener when channel message is error component then process message")
    void receiveMessageFromListenerWhenChannelMessageIsErrorComponentThenProcessMessage()  {
        ChannelMessage channelMessageErrorComponent = Ss7Utils.createChannelMessage(Constants.ON_ERROR_COMPONENT);
        var dialogErrorComponent = new MAPErrorMessageAbsentSubscriberSMImpl();
        channelMessageErrorComponent.setParameter(Constants.DIALOG, mapDialogSms);
        channelMessageErrorComponent.setParameter(Constants.INVOKE_ID, mapDialogSms.getLocalDialogId());
        channelMessageErrorComponent.setParameter(Constants.MAP_ERROR_MESSAGE, dialogErrorComponent);
        when(mapDialogSms.getLocalDialogId()).thenReturn(10L);
        var mapApplicationContext = MAPApplicationContext
                .getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.getInstance(3));
        when(mapDialogSms.getApplicationContext()).thenReturn(mapApplicationContext);
        mapChannel.receiveMessageFromListener(channelMessageErrorComponent);
        verify(messageProcessing).processError(channelMessageErrorComponent, Constants.ON_ERROR_COMPONENT);
    }

    @Test
    @DisplayName("Receive message from listener when channel message is invoke timeout then process message")
    void receiveMessageFromListenerWhenChannelMessageIsInvokeTimeoutThenProcessMessage()  {
        ChannelMessage channelMessageInvokeTimeout = Ss7Utils.createChannelMessage(Constants.ON_INVOKE_TIMEOUT);
        channelMessageInvokeTimeout.setParameter(Constants.DIALOG, mapDialogSms);
        channelMessageInvokeTimeout.setParameter(Constants.INVOKE_ID, mapDialogSms.getLocalDialogId());
        when(mapDialogSms.getLocalDialogId()).thenReturn(10L);
        var mapApplicationContext = MAPApplicationContext
                .getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.getInstance(3));
        when(mapDialogSms.getApplicationContext()).thenReturn(mapApplicationContext);
        mapChannel.receiveMessageFromListener(channelMessageInvokeTimeout);
        verify(messageProcessing).processError(channelMessageInvokeTimeout, Constants.ON_INVOKE_TIMEOUT);
    }

    @Test
    @DisplayName("Receive message from listener when channel message is null then do nothing")
    void receiveMessageFromListenerWhenChannelMessageIsNullThenDoNothing()  {
        ChannelMessage channelMessageWithNullDialog = Ss7Utils.createChannelMessage(Constants.ON_DIALOG_CLOSE);
        channelMessageWithNullDialog.setParameter(Constants.DIALOG, null);
        mapChannel.receiveMessageFromListener(channelMessageWithNullDialog);
        verifyNoInteractions(messageProcessing);
    }

    @Test
    @DisplayName("Receive message from listener when channel message is unknown then do nothing")
    void receiveMessageFromListenerWhenChannelMessageIsUnknownThenDoNothing()  {
        ChannelMessage channelMessageWithNullDialog = Ss7Utils.createChannelMessage(Constants.ON_REJECT_COMPONENT);
        channelMessageWithNullDialog.setParameter(Constants.DIALOG, mapDialogSms);
        mapChannel.receiveMessageFromListener(channelMessageWithNullDialog);
        verifyNoInteractions(messageProcessing);
    }
}