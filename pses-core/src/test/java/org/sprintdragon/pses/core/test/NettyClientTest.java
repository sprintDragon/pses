package org.sprintdragon.pses.core.test;

import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.client.NettyClient;
import org.sprintdragon.pses.core.transport.netty4.client.NettyClientFactory;

import java.util.concurrent.ExecutionException;

/**
 * Created by wangdi on 17-8-2.
 */
public class NettyClientTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        NettyClient client = NettyClientFactory.get();
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(System.currentTimeMillis());
        rpcRequest.setActionName("xxx");
        RpcResponse rpcResponse = client.send(rpcRequest).get();
        System.out.println("##" + rpcResponse);
        client.close();
    }

}
