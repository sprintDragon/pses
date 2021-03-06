package org.sprintdragon.pses.core.transport.netty4.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.action.ActionFuture;
import org.sprintdragon.pses.core.action.ActionListenerResponseHandler;
import org.sprintdragon.pses.core.action.supprot.AdapterActionFuture;
import org.sprintdragon.pses.core.action.supprot.CompletableActionFuture;
import org.sprintdragon.pses.core.action.supprot.PlainActionFuture;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by wangdi on 17-8-3.
 */
@Component
@Slf4j
public class TransportClient extends AbstractLifecycleComponent {

    @Resource
    TransportService transportService;
    private DiscoveryNode namedNode;

    public TransportClient(Settings settings) {
        super(settings);
    }

    public void setNamedNode(DiscoveryNode namedNode) {
        this.namedNode = namedNode;
    }

    public ActionFuture<RpcResponse> sendRequest(DiscoveryNode node, RpcRequest request) {
        log.info("sendRequest,node={},request={}", node, request);
        CompletableActionFuture<RpcResponse> actionFuture = CompletableActionFuture.newFuture();
        execute(node, request, actionFuture);
        return actionFuture;
    }

    private void execute(DiscoveryNode node, RpcRequest request, CompletableActionFuture<RpcResponse> actionFuture) {
        transportService.sendRequest(node, request, new ActionListenerResponseHandler<RpcResponse>(actionFuture) {
            @Override
            public RpcResponse newInstance() {
                return new RpcResponse();
            }
        });
    }

    @Override
    protected void doStart() throws Exception {
        transportService.connectToNode(namedNode);
    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() throws IOException {
        transportService.close();
    }
}
