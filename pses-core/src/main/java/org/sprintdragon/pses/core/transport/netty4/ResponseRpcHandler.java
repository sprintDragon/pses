package org.sprintdragon.pses.core.transport.netty4;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.transport.TcpTransport;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

/**
 * Created by patterncat on 2016/4/6.
 */
@Component
@ChannelHandler.Sharable
public class ResponseRpcHandler extends SimpleMessageHandler<RpcResponse> {

    public ResponseRpcHandler(TcpTransport transport) {
        super(transport);
    }

    //messageReceived
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        transport.handlerResponse(getRemoteAddress(ctx), profileName, rpcResponse);
    }

}
