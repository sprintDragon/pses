package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * Created by wangdi on 17-8-4.
 */
@Slf4j
public abstract class SimpleMessageHandler<T> extends SimpleChannelInboundHandler<T> {

    @Resource
    TransportService transportService;
    protected String profileName;

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public void handlerMessage(ChannelHandlerContext ctx, T t) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (t instanceof RpcResponse) {
            transportService.handlerResponse(ctx.channel(), remoteAddress, profileName, (RpcResponse) t);
        } else if (t instanceof RpcRequest) {
            transportService.handlerReuest(ctx.channel(), remoteAddress, profileName, (RpcRequest) t);
        } else {
            log.warn("handlerMessage unknown type msg to handle t=", t);
        }


    }

}
