package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;

/**
 * Created by patterncat on 2016/4/6.
 */
@Component
@Slf4j
@ChannelHandler.Sharable
public class RequestRpcHandler extends SimpleMessageHandler<RpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        transportService.handlerReuest(ctx.channel(), getRemoteAddress(ctx), profileName, rpcRequest);
    }

}
