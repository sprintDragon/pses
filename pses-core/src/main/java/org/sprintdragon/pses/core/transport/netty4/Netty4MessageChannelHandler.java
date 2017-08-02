package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by wangdi on 17-8-2.
 */
public class Netty4MessageChannelHandler extends ChannelDuplexHandler {

    private final Netty4Transport transport;
    private final String profileName;

    public Netty4MessageChannelHandler(Netty4Transport transport, String profileName) {
        this.transport = transport;
        this.profileName = profileName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("###" + msg);
        super.channelRead(ctx, msg);
    }
}
