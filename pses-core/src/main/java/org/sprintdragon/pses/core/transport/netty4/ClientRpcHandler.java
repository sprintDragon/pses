package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.transport.TransportResponseHandler;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import javax.annotation.Resource;

/**
 * Created by patterncat on 2016/4/6.
 */
@Component
@ChannelHandler.Sharable
public class ClientRpcHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Resource
    TransportService transportService;

    //messageReceived
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        System.out.println("receive response:" + rpcResponse);
        TransportResponseHandler handler = transportService.onResponseReceived(rpcResponse.getRequestId());
        handler.handleResponse(rpcResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }

}
