package com.paicbd.module.ss7.layer.api.channel;

import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;

public interface IChannelHandler {

    /**
     * Receive the message from the listener
     *
     * @param channelMessage ChannelMessage
     */
    void receiveMessageFromListener(ChannelMessage channelMessage);

}
