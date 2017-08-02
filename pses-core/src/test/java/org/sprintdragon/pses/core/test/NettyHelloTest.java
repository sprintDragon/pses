package org.sprintdragon.pses.core.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.sprintdragon.pses.core.PsesServerApplication;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.client.NettyClient;

import javax.annotation.Resource;

/**
 * Created by wangdi on 17-7-28.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PsesServerApplication.class, value = "spring.profiles.active=dev")
public class NettyHelloTest {

    @Resource
    NettyClient client;

    @Test
    public void testHello() throws Exception {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(System.currentTimeMillis());
        rpcRequest.setActionName("xxx");
        RpcResponse rpcResponse = client.send(rpcRequest).get();
        System.out.println("##" + rpcResponse);
        client.close();
    }

}
