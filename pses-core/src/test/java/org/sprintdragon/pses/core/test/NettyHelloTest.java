package org.sprintdragon.pses.core.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.sprintdragon.pses.PsesServerApplication;
import org.sprintdragon.pses.core.action.get.GetAction;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.client.TransportClient;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * Created by wangdi on 17-7-28.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PsesServerApplication.class, value = "spring.profiles.active=dev")
public class NettyHelloTest {

    @Resource
    TransportClient client;
    DiscoveryNode namedNode;

    @Test
    public void testHello() throws Exception {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setAction(GetAction.NAME);
        RpcResponse rpcResponse = client.sendRequest(namedNode, rpcRequest).get();
        System.out.println("##" + rpcResponse);
    }

    @Before
    public void setUp() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.1.2", 9090);
        namedNode = new DiscoveryNode();
        namedNode.setAddress(inetSocketAddress);
        namedNode.setNodeId("s");
        client.setNamedNode(namedNode);
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }
}
