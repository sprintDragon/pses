package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import javax.annotation.Resource;

/**
 * Created by patterncat on 2016/4/6.
 */
@Component
@Slf4j
@ChannelHandler.Sharable
public class RequestRpcHandler extends SimpleMessageHandler<RpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        handlerMessage(channelHandlerContext, rpcRequest);
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
