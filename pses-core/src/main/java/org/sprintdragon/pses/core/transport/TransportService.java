package org.sprintdragon.pses.core.transport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.common.util.concurrent.FutureUtils;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.exception.SendRequestTransportException;
import org.sprintdragon.pses.core.transport.exception.TransportException;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
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
    ScheduledThreadPoolExecutor schThreadPool = new ScheduledThreadPoolExecutor(5);
    //    protected final TcpTransport transport;
    final ConcurrentMap<Long, RequestHolder> clientHandlers = new ConcurrentHashMap<>();

    final CopyOnWriteArrayList<TransportConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    // An LRU (don't really care about concurrency here) that holds the latest timed out requests so if they
    // do show up, we can print more descriptive information about them
    final Map<Long, TimeoutInfoHolder> timeoutInfoHandlers = Collections.synchronizedMap(new LinkedHashMap<Long, TimeoutInfoHolder>(100, .75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 100;
        }
    });

    /**
     * ImmutableMap 不可变集合
     */
    volatile ImmutableMap<String, RequestHandlerRegistry> requestHandlers = ImmutableMap.of();

    final Object requestHandlerMutex = new Object();

    final AtomicLong requestIds = new AtomicLong();
    private TransportService.Adapter adapter;
    @Resource
    private Transport transport;

    public TransportService(Settings settings) {
        super(settings);
    }

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
        transport.connectToNode(node, connection -> {
            //todo handshake
        });
    }

    public void disconnectFromNode(DiscoveryNode node) throws Exception {
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

    //listener封装进了handler
    public <T extends RpcResponse> void sendRequest(DiscoveryNode node, final RpcRequest request,
                                                    TransportResponseHandler<T> handler) {
        if (node == null) {
            throw new IllegalStateException("can't send request to a null node");
        }
        final long requestId = newRequestId();
        final TimeoutHandler timeoutHandler;
        try {
            long timeoutMill = request.getTimeout();
            if (timeoutMill > 0) {
                timeoutHandler = new TimeoutHandler(requestId);
            } else {
                timeoutHandler = null;
            }
            request.setRequestId(requestId);
            clientHandlers.put(requestId, new RequestHolder<>(handler, new DiscoveryNode(), request.getAction(), timeoutHandler));
            if (started.get() == false) {
                // if we are not started the exception handling will remove the RequestHolder again and calls the handler to notify the caller.
                // it will only notify if the toStop code hasn't done the work yet.
                throw new TransportException(node, "TransportService is closed stopped can't send request");
            }

            if (timeoutHandler != null) {
                assert timeoutMill > 0;
                timeoutHandler.future = schThreadPool.schedule(timeoutHandler, timeoutMill, TimeUnit.MILLISECONDS);
            }


            Transport.Connection connection = transport.getConnection(node);
            connection.sendRequest(request);
        } catch (Throwable e) {
            log.error("sendRequest error", e);
            // usually happen either because we failed to connect to the node
            // or because we failed serializing the message
            final RequestHolder holderToNotify = clientHandlers.remove(requestId);
            // If holderToNotify == null then handler has already been taken care of.
            if (holderToNotify != null) {
                holderToNotify.cancelTimeout();
                // callback that an exception happened, but on a different thread since we don't
                // want handlers to worry about stack overflows
                final SendRequestTransportException sendRequestException = new SendRequestTransportException(node, e);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        holderToNotify.handler().handleException(sendRequestException);
                    }
                }).start();
            }
        }
    }

    private long newRequestId() {
        return requestIds.getAndIncrement();
    }

    class TimeoutHandler implements Runnable {

        private final long requestId;

        private final long sentTime = System.currentTimeMillis();

        volatile ScheduledFuture future;

        TimeoutHandler(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public void run() {
            // we get first to make sure we only add the TimeoutInfoHandler if needed.
            final RequestHolder holder = clientHandlers.get(requestId);
            if (holder != null) {
                // add it to the timeout information holder, in case we are going to get a response later
                long timeoutTime = System.currentTimeMillis();
                timeoutInfoHandlers.put(requestId, new TimeoutInfoHolder(holder.node(), holder.action(), sentTime, timeoutTime));
                // now that we have the information visible via timeoutInfoHandlers, we try to remove the request id
                final RequestHolder removedHolder = clientHandlers.remove(requestId);
                if (removedHolder != null) {
                    assert removedHolder == holder : "two different holder instances for request [" + requestId + "]";
                    removedHolder.handler().handleException(new TransportException(holder.node, "request_id [" + requestId + "] timed out after [" + (timeoutTime - sentTime) + "ms]"));
                } else {
                    // response was processed, remove timeout info.
                    timeoutInfoHandlers.remove(requestId);
                }
            }
        }

        /**
         * cancels timeout handling. this is a best effort only to avoid running it. remove the requestId from {@link #clientHandlers}
         * to make sure this doesn't run.
         */
        public void cancel() {
            assert clientHandlers.get(requestId) == null : "cancel must be called after the requestId [" + requestId + "] has been removed from clientHandlers";
            FutureUtils.cancel(future);
        }
    }

    static class TimeoutInfoHolder {

        private final DiscoveryNode node;
        private final String action;
        private final long sentTime;
        private final long timeoutTime;

        TimeoutInfoHolder(DiscoveryNode node, String action, long sentTime, long timeoutTime) {
            this.node = node;
            this.action = action;
            this.sentTime = sentTime;
            this.timeoutTime = timeoutTime;
        }

        public DiscoveryNode node() {
            return node;
        }

        public String action() {
            return action;
        }

        public long sentTime() {
            return sentTime;
        }

        public long timeoutTime() {
            return timeoutTime;
        }
    }

    static class RequestHolder<T extends RpcResponse> {

        private final TransportResponseHandler<T> handler;

        private final DiscoveryNode node;

        private final String action;

        private final TimeoutHandler timeoutHandler;

        RequestHolder(TransportResponseHandler<T> handler, DiscoveryNode node, String action, TimeoutHandler timeoutHandler) {
            this.handler = handler;
            this.node = node;
            this.action = action;
            this.timeoutHandler = timeoutHandler;
        }

        public TransportResponseHandler<T> handler() {
            return handler;
        }

        public DiscoveryNode node() {
            return this.node;
        }

        public String action() {
            return this.action;
        }

        public void cancelTimeout() {
            if (timeoutHandler != null) {
                timeoutHandler.cancel();
            }
        }

    }

    protected class Adapter implements TransportServiceAdapter {

        @Override
        public void onRequestReceived(long requestId, String action) {
            log.info("onRequestReceived requestId={},action={}", requestId, action);
        }

        @Override
        public TransportResponseHandler onResponseReceived(final long requestId) {
            RequestHolder holder = clientHandlers.remove(requestId);
            if (holder == null) {
                checkForTimeout(requestId);
                return null;
            }
            holder.cancelTimeout();
            return holder.handler();
        }

        @Override
        public RequestHandlerRegistry getRequestHandler(String action) {
            return requestHandlers.get(action);
        }

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
                    }).start();
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

        protected void checkForTimeout(long requestId) {
            // lets see if its in the timeout holder, but sync on mutex to make sure any ongoing timeout handling has finished
            final DiscoveryNode sourceNode;
            final String action;
            assert clientHandlers.get(requestId) == null;
            TimeoutInfoHolder timeoutInfoHolder = timeoutInfoHandlers.remove(requestId);
            if (timeoutInfoHolder != null) {
                long time = System.currentTimeMillis();
                log.warn("Received response for a request that has timed out, sent [{}ms] ago, timed out [{}ms] ago, action [{}], node [{}], id [{}]", time - timeoutInfoHolder.sentTime(), time - timeoutInfoHolder.timeoutTime(), timeoutInfoHolder.action(), timeoutInfoHolder.node(), requestId);
            } else {
                log.warn("Transport response handler not found of id [{}]", requestId);
            }
        }
    }


    /**
     * Registers a new request handler
     *
     * @param action  The action the request handler is associated with
     * @param request The request class that will be used to constrcut new instances for streaming
     * @param handler The handler itself that implements the request handling
     */
    public final <Request extends RpcRequest> void registerRequestHandler(String action, Class<Request> request, String executor, TransportRequestHandler<Request> handler) {
        registerRequestHandler(action, request, executor, false, handler);
    }


    /**
     * Registers a new request handler
     *
     * @param action         The action the request handler is associated with
     * @param requestFactory a callable to be used construct new instances for streaming
     * @param handler        The handler itself that implements the request handling
     */
    public <Request extends RpcRequest> void registerRequestHandler(String action, Callable<Request> requestFactory, String executor, TransportRequestHandler<Request> handler) {
        RequestHandlerRegistry<Request> reg = new RequestHandlerRegistry<>(action, requestFactory, handler, executor, false);
        registerRequestHandler(reg);
    }

    /**
     * Registers a new request handler
     *
     * @param action         The action the request handler is associated with
     * @param request        The request class that will be used to constrcut new instances for streaming
     * @param forceExecution Force execution on the executor queue and never reject it
     * @param handler        The handler itself that implements the request handling
     */
    public <Request extends RpcRequest> void registerRequestHandler(String action, Class<Request> request, String executor, boolean forceExecution, TransportRequestHandler<Request> handler) {
        RequestHandlerRegistry<Request> reg = new RequestHandlerRegistry<>(action, request, handler, executor, forceExecution);
        registerRequestHandler(reg);
    }

    /**
     * 注册请求处理器
     *
     * @param reg
     * @param <Request>
     */
    protected <Request extends RpcRequest> void registerRequestHandler(RequestHandlerRegistry<Request> reg) {
        synchronized (requestHandlerMutex) {
            RequestHandlerRegistry replaced = requestHandlers.get(reg.getAction());
            Map map = Maps.newHashMap();
            map.put(reg.getAction(), reg);
            requestHandlers = ImmutableMap.copyOf(map);
            if (replaced != null) {
                log.warn("registered two transport handlers for action {}, handlers: {}, {}", reg.getAction(), reg.getHandler(), replaced.getHandler());
            }
        }
    }

}
