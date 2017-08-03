package org.sprintdragon.pses.core.transport;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wangdi on 17-8-1.
 */
@Component
@Slf4j
public class TransportService extends AbstractLifecycleComponent {

    //    volatile DiscoveryNode localNode = null;
    private final AtomicBoolean started = new AtomicBoolean(false);
    //    protected final TcpTransport transport;
    final ConcurrentMap<Long, RequestHolder> clientHandlers = new ConcurrentHashMap<>();

    final Object requestHandlerMutex = new Object();

    final AtomicLong requestIds = new AtomicLong();

//    volatile ImmutableMap<String, RequestHandlerRegistry> requestHandlers = ImmutableMap.of();

    @Override
    protected void doStart() {
        started.compareAndSet(false, true);
    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }

    public <T extends RpcResponse> void sendRequest(Channel channel, final RpcRequest request,
                                                    TransportResponseHandler<T> handler) {
//        if (node == null) {
//            throw new IllegalStateException("can't send request to a null node");
//        }
        final long requestId = newRequestId();
        try {
            clientHandlers.put(requestId, new RequestHolder<>(handler));
            if (started.get() == false) {
                // if we are not started the exception handling will remove the RequestHolder again and calls the handler to notify the caller.
                // it will only notify if the toStop code hasn't done the work yet.
                throw new RuntimeException("TransportService is closed stopped can't send request");
            }

            channel.writeAndFlush(request);
        } catch (Exception e) {
            log.error("sendRequest error", e);
        }
    }

    private long newRequestId() {
        return requestIds.getAndIncrement();
    }

    static class RequestHolder<T extends RpcResponse> {

        private final TransportResponseHandler<T> handler;

//        private final DiscoveryNode node;

//        private final String action;

        RequestHolder(TransportResponseHandler<T> handler) {
            this.handler = handler;
//            this.node = node;
//            this.action = action;
        }

        public TransportResponseHandler<T> handler() {
            return handler;
        }

//        public DiscoveryNode node() {
//            return this.node;
//        }
//
//        public String action() {
//            return this.action;
//        }

    }

    public TransportResponseHandler onResponseReceived(final long requestId) {
        RequestHolder holder = clientHandlers.remove(requestId);
//        if (holder == null) {
//            checkForTimeout(requestId);
//            return null;
//        }
//        holder.cancelTimeout();
        return holder.handler();
    }

}
