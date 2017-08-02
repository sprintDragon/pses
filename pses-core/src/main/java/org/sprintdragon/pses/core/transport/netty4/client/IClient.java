package org.sprintdragon.pses.core.transport.netty4.client;

import org.sprintdragon.pses.core.action.ActionFuture;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import java.net.InetSocketAddress;

public interface IClient {

    void connect(InetSocketAddress socketAddress);

    ActionFuture<RpcResponse> send(RpcRequest request) throws InterruptedException;

    InetSocketAddress getRemoteAddress();

    void close();
}
