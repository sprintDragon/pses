package org.sprintdragon.pses.core.transport.netty4;

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
public class ServerRpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    @Resource
    TransportService transportService;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        RpcResponse response = new RpcResponse();
        try {
            log.info("server handle request:{}", rpcRequest);
            response.setRequestId(rpcRequest.getRequestId());
            response.setResult("success!!!");
            transportService.onResponseReceived(rpcRequest.getRequestId());
//            Object result = handle(rpcRequest);
//            response.setResult(result);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            response.setError(t);
        }
        channelHandlerContext.writeAndFlush(response);
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
