package org.sprintdragon.pses.core.test;

import io.netty.channel.Channel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.sprintdragon.pses.core.PsesServerApplication;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.Netty4Transport;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * Created by wangdi on 17-7-28.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PsesServerApplication.class, value = "spring.profiles.active=dev")
public class NettyHelloTest {

    @Resource
    Netty4Transport transport;

    @Test
    public void testHello() throws Exception {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setActionName("xxx");
        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.1.2", 9090);
        Channel channel = transport.doConnect(inetSocketAddress);
        RpcResponse rpcResponse = transport.sendRequest(channel, rpcRequest).get();
        System.out.println("##" + rpcResponse);
    }

}
