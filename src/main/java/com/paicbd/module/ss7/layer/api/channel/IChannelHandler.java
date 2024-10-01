package com.paicbd.module.ss7.layer.api.channel;

import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.smsc.dto.MessageEvent;

public interface IChannelHandler {
    /**
     * This function is called to initialize the channel
     */
    void channelInitialize(ILayer iLayer);

    /**
     * Send Channel message from the channel to the application
     *
     * @param channelMessage ChannelMessage
     */
    void receiveMessageFromListener(ChannelMessage channelMessage);


    void sendMessage(MessageEvent message);

}
