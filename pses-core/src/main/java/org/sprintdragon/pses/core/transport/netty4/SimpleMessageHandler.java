package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.transport.TcpTransport;
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

    protected TcpTransport transport;
    protected String profileName;

    public SimpleMessageHandler(TcpTransport transport) {
        this.transport = transport;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    protected InetSocketAddress getRemoteAddress(ChannelHandlerContext ctx) {
        return (InetSocketAddress) ctx.channel().remoteAddress();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        RpcResponse response = new RpcResponse();
//        if(cause instanceof ServerException){
//            response.setTraceId(((ServerException) cause).getTraceId());
//        }
        response.setError(cause);
        ctx.writeAndFlush(response);
    }

}
