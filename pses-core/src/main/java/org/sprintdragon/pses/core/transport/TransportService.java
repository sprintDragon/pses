package org.sprintdragon.pses.core.transport;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wangdi on 17-8-1.
 */
@Component
@Slf4j
public class TransportService extends AbstractLifecycleComponent implements InitializingBean {

    //    volatile DiscoveryNode localNode = null;
    private final AtomicBoolean started = new AtomicBoolean(false);
    protected volatile TransportServiceAdapter transportServiceAdapter;
    //    protected final TcpTransport transport;
    final ConcurrentMap<Long, RequestHolder> clientHandlers = new ConcurrentHashMap<>();

    final CopyOnWriteArrayList<TransportConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    final Object requestHandlerMutex = new Object();

    final AtomicLong requestIds = new AtomicLong();
    private TransportService.Adapter adapter;
    @Resource
    private Transport transport;

//    volatile ImmutableMap<String, RequestHandlerRegistry> requestHandlers = ImmutableMap.of();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.adapter = createAdapter();
    }

    protected Adapter createAdapter() {
        return new Adapter();
    }

    public boolean nodeConnected(DiscoveryNode node) {
        return transport.nodeConnected(node);
    }

    public void connectToNode(DiscoveryNode node) throws Exception {
        transport.connectToNode(node);
    }

    public void disconnectFromNode(DiscoveryNode node) {
        transport.disconnectFromNode(node);
    }

    @Override
    protected void doStart() {
        transport.setTransportServiceAdapter(adapter);
        started.compareAndSet(false, true);
    }

    @Override
    protected void doStop() {
        transport.stop();
        started.compareAndSet(true, false);
    }

    @Override
    protected void doClose() {

    }

    public <T extends RpcResponse> void sendRequest(DiscoveryNode node, final RpcRequest request,
                                                    TransportResponseHandler<T> handler) {
//        if (node == null) {
//            throw new IllegalStateException("can't send request to a null node");
//        }
        final long requestId = newRequestId();
        try {
            request.setRequestId(requestId);
            clientHandlers.put(requestId, new RequestHolder<>(handler, new DiscoveryNode()));
            if (started.get() == false) {
                // if we are not started the exception handling will remove the RequestHolder again and calls the handler to notify the caller.
                // it will only notify if the toStop code hasn't done the work yet.
                throw new RuntimeException("TransportService is closed stopped can't send request");
            }

            Channel targetChannel = transport.nodeChannel(node);
            targetChannel.writeAndFlush(request);
        } catch (Exception e) {
            log.error("sendRequest error", e);
        }
    }

    private long newRequestId() {
        return requestIds.getAndIncrement();
    }

    static class RequestHolder<T extends RpcResponse> {

        private final TransportResponseHandler<T> handler;

        private final DiscoveryNode node;

//        private final String action;

        RequestHolder(TransportResponseHandler<T> handler, DiscoveryNode node) {
            this.handler = handler;
            this.node = node;
//            this.action = action;
        }

        public TransportResponseHandler<T> handler() {
            return handler;
        }

        public DiscoveryNode node() {
            return this.node;
        }
//
//        public String action() {
//            return this.action;
//        }

    }

    public TransportResponseHandler onResponseReceived(final Long requestId) {
        RequestHolder holder = clientHandlers.remove(requestId);
//        if (holder == null) {
//            checkForTimeout(requestId);
//            return null;
//        }
//        holder.cancelTimeout();
        return holder.handler();
    }

    protected class Adapter implements TransportServiceAdapter {

        @Override
        public void raiseNodeConnected(DiscoveryNode node) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (TransportConnectionListener connectionListener : connectionListeners) {
                        connectionListener.onNodeConnected(node);
                    }
                }
            }).start();
        }

        @Override
        public void raiseNodeDisconnected(DiscoveryNode node) {
            try {
                for (final TransportConnectionListener connectionListener : connectionListeners) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            connectionListener.onNodeDisconnected(node);
                        }
                    });
                }
                for (Map.Entry<Long, RequestHolder> entry : clientHandlers.entrySet()) {
                    RequestHolder holder = entry.getValue();
                    if (holder.node().equals(node)) {
                        final RequestHolder holderToNotify = clientHandlers.remove(entry.getKey());
                        if (holderToNotify != null) {
                            // callback that an exception happened, but on a different thread since we don't
                            // want handlers to worry about stack overflows
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //TODO handle exception
//                                    holderToNotify.handler().handleException(new NodeDisconnectedException(node, holderToNotify.action()));
                                }
                            }).start();
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Rejected execution on NodeDisconnected", ex);
            }
        }
    }

}
