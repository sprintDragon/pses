package org.sprintdragon.pses.core.transport;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.util.IOUtils;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.CheckedBiConsumer;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.common.lease.Releasable;
import org.sprintdragon.pses.core.common.metrics.CounterMetric;
import org.sprintdragon.pses.core.common.network.NetworkService;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.common.util.concurrent.KeyedLock;
import org.sprintdragon.pses.core.transport.dto.RpcMessage;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.exception.TransportException;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Created by wangdi on 17-8-2.
 */
@Slf4j
public abstract class TcpTransport<Channel> extends AbstractLifecycleComponent implements Transport {

    @Resource
    protected NetworkService networkService;
    private final ConcurrentMap<Long, HandshakeResponseHandler> pendingHandshakes = new ConcurrentHashMap<>();
    protected final Map<String, List<Channel>> serverChannels = new ConcurrentHashMap<>();
    protected volatile BoundTransportAddress boundAddress;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    protected volatile TransportServiceAdapter transportServiceAdapter;
    // node id to actual channel
    protected final ConcurrentMap<DiscoveryNode, NodeChannel> connectedNodes = new ConcurrentHashMap<>();
    private final Set<NodeChannel> openConnections = new ConcurrentSkipListSet<>();
    protected final KeyedLock<String> connectionLock = new KeyedLock<>();
    private final CounterMetric numHandshakes = new CounterMetric();
    private final AtomicLong requestIdGenerator = new AtomicLong();

    @Override
    public void setTransportServiceAdapter(TransportServiceAdapter transportServiceAdapter) {
        this.transportServiceAdapter = transportServiceAdapter;
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return this.boundAddress;
    }

    @Override
    public boolean nodeConnected(DiscoveryNode node) {
        return connectedNodes.containsKey(node);
    }

    protected abstract NodeChannel connectToChannel(DiscoveryNode node, Consumer<Channel> onChannelClose) throws IOException;

    @Override
    public void connectToNode(DiscoveryNode node, CheckedBiConsumer connectionValidator) throws Exception {
        if (node == null) {
            throw new RuntimeException("can't connect to a null node");
        }
        globalLock.readLock().lock(); // ensure we don't open connections while we are closing
        try {
            ensureOpen();
            try (Releasable ignored = connectionLock.acquire(node.id())) {
                NodeChannel nodeChannel = connectedNodes.get(node);
                if (nodeChannel != null) {
                    return;
                }
                boolean success = false;
                try {
                    nodeChannel = openConnection(node);
                    connectionValidator.accept(nodeChannel);
                    // we acquire a connection lock, so no way there is an existing connection
                    connectedNodes.put(node, nodeChannel);
                    if (log.isDebugEnabled()) {
                        log.debug("connected to node [{}]", node);
                    }
                    transportServiceAdapter.onNodeConnected(node);
                    success = true;
                } catch (Exception e) {
                    throw new RuntimeException("general node connection failure", e);
                } finally {
                    if (success == false) { // close the connection if there is a failure
                        log.trace("failed to connect to [{}], cleaning dangling connections", node);
                        IOUtils.closeWhileHandlingException(nodeChannel);
                    }
                }
            }
        } finally {
            globalLock.readLock().unlock();
        }
    }


    /**
     * Disconnects from a node, only if the relevant channel is found to be part of the node channels.
     */
    protected boolean disconnectFromNode(DiscoveryNode node, Channel channel, String reason) {
        // this might be called multiple times from all the node channels, so do a lightweight
        // check outside of the lock
        NodeChannel nodeChannels = connectedNodes.get(node);
        if (nodeChannels != null) {
            try (Releasable ignored = connectionLock.acquire(node.id())) {
                nodeChannels = connectedNodes.get(node);
                // check again within the connection lock, if its still applicable to remove it
                if (nodeChannels != null) {
                    connectedNodes.remove(node);
                    closeAndNotify(node, nodeChannels, reason);
                    return true;
                }
            }
        }
        return false;
    }

    private void closeAndNotify(DiscoveryNode node, NodeChannel nodeChannel, String reason) {
        try {
            log.debug("disconnecting from [{}], {}", node, reason);
            IOUtils.closeWhileHandlingException(nodeChannel);
        } finally {
            log.trace("disconnected from [{}], {}", node, reason);
            transportServiceAdapter.onNodeDisconnected(node);
        }
    }


    @Override
    public void disconnectFromNode(DiscoveryNode node) throws Exception {
        try (Releasable ignored = connectionLock.acquire(node.id())) {
            NodeChannel nodeChannels = connectedNodes.remove(node);
            if (nodeChannels != null) {
                closeAndNotify(node, nodeChannels, "due to explicit disconnect call");
            }
        }
    }

    @Override
    public NodeChannel openConnection(DiscoveryNode node) throws IOException {
        if (node == null) {
            throw new RuntimeException("can't open connection to a null node");
        }
        boolean success = false;
        NodeChannel nodeChannel = null;
        globalLock.readLock().lock(); // ensure we don't open connections while we are closing
        try {
            ensureOpen();
            try {
                AtomicBoolean runOnce = new AtomicBoolean(false);
                Consumer<Channel> onClose = c -> {
                    try {
                        // we can't assert that the channel is closed here since netty3 has a different behavior that doesn't
                        // consider a channel closed while it's close future is running.
                        onChannelClosed(c);
                    } finally {
                        // we only need to disconnect from the nodes once since all other channels
                        // will also try to run this we protect it from running multiple times.
                        if (runOnce.compareAndSet(false, true)) {
                            disconnectFromNodeChannel(c, "channel closed");
                        }
                    }
                };
                nodeChannel = connectToChannel(node, onClose);
//                final Channel channel = nodeChannel.channel; // one channel is guaranteed by the connection profile
//                final TimeValue connectTimeout = connectionProfile.getConnectTimeout() == null ?
//                        defaultConnectionProfile.getConnectTimeout() :
//                        connectionProfile.getConnectTimeout();
//                final TimeValue handshakeTimeout = connectionProfile.getHandshakeTimeout() == null ?
//                        connectTimeout : connectionProfile.getHandshakeTimeout();
//                final Version version = executeHandshake(node, channel, 5000);
//                if (version != null) {
                // if we are talking to a pre 5.2 node we won't be able to retrieve the version since it doesn't implement the handshake
                // we do since 5.2 - in this case we just go with the version provided by the node.
//                    nodeChannel = new NodeChannel(node, channel); // clone the channels - we now have the correct version
//                }
                transportServiceAdapter.onConnectionOpened(nodeChannel);
                openConnections.add(nodeChannel);
                success = true;
                return nodeChannel;
            } catch (Exception e) {
                // ConnectTransportExceptions are handled specifically on the caller end - we wrap the actual exception to ensure
                // only relevant exceptions are logged on the caller end.. this is the same as in connectToNode
                throw new RuntimeException("general node connection failure", e);
            } finally {
                if (success == false) {
                    IOUtils.closeWhileHandlingException(nodeChannel);
                }
            }
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public long newRequestId() {
        return requestIdGenerator.incrementAndGet();
    }

//    protected Version executeHandshake(DiscoveryNode node, Channel channel, long timeout) throws IOException, InterruptedException {
//        numHandshakes.inc();
//        final long requestId = newRequestId();
//        final HandshakeResponseHandler handler = new HandshakeResponseHandler(channel);
//        AtomicReference<Version> versionRef = handler.versionRef;
//        AtomicReference<Exception> exceptionRef = handler.exceptionRef;
//        pendingHandshakes.put(requestId, handler);
//        boolean success = false;
//        try {
//            if (isOpen(channel) == false) {
//                // we have to protect us here since sendRequestToChannel won't barf if the channel is closed.
//                // it's weird but to change it will cause a lot of impact on the exception handling code all over the codebase.
//                // yet, if we don't check the state here we might have registered a pending handshake handler but the close
//                // listener calling #onChannelClosed might have already run and we are waiting on the latch below unitl we time out.
//                throw new IllegalStateException("handshake failed, channel already closed");
//            }
//            // for the request we use the minCompatVersion since we don't know what's the version of the node we talk to
//            // we also have no payload on the request but the response will contain the actual version of the node we talk
//            // to as the payload.
//            final Version minCompatVersion = getCurrentVersion().minimumCompatibilityVersion();
//            sendRequestToChannel(node, channel, requestId, HANDSHAKE_ACTION_NAME, TransportRequest.Empty.INSTANCE,
//                    TransportRequestOptions.EMPTY, minCompatVersion, TransportStatus.setHandshake((byte) 0));
//            if (handler.latch.await(timeout, TimeUnit.MILLISECONDS) == false) {
//                throw new RuntimeException( "handshake_timeout[" + timeout + "]");
//            }
//            success = true;
//            if (handler.handshakeNotSupported.get()) {
//                // this is a BWC layer, if we talk to a pre 5.2 node then the handshake is not supported
//                // this will go away in master once it's all ported to 5.2 but for now we keep this to make
//                // the backport straight forward
//                return null;
//            }
//            if (exceptionRef.get() != null) {
//                throw new IllegalStateException("handshake failed", exceptionRef.get());
//            } else {
//                Version version = versionRef.get();
//                if (getCurrentVersion().isCompatible(version) == false) {
//                    throw new IllegalStateException("Received message from unsupported version: [" + version
//                            + "] minimal compatible version is: [" + getCurrentVersion().minimumCompatibilityVersion() + "]");
//                }
//                return version;
//            }
//        } finally {
//            final TransportResponseHandler<?> removedHandler = pendingHandshakes.remove(requestId);
//            // in the case of a timeout or an exception on the send part the handshake has not been removed yet.
//            // but the timeout is tricky since it's basically a race condition so we only assert on the success case.
//            assert success && removedHandler == null || success == false : "handler for requestId [" + requestId + "] is not been removed";
//        }
//    }

    /**
     * Called once the channel is closed for instance due to a disconnect or a closed socket etc.
     */
    private void onChannelClosed(Channel channel) {
        final Optional<Long> first = pendingHandshakes.entrySet().stream()
                .filter((entry) -> entry.getValue().channel == channel).map((e) -> e.getKey()).findFirst();
        if (first.isPresent()) {
            final Long requestId = first.get();
            final HandshakeResponseHandler handler = pendingHandshakes.remove(requestId);
            if (handler != null) {
                // there might be a race removing this or this method might be called twice concurrently depending on how
                // the channel is closed ie. due to connection reset or broken pipes
                handler.handleException(new TransportException(null, "connection reset"));
            }
        }
    }

    protected abstract boolean isOpen(Channel channel);

    /**
     * Disconnects from a node if a channel is found as part of that nodes channels.
     */
    protected final void disconnectFromNodeChannel(final Channel channel, final String reason) {
        new Thread(() -> {
            try {
                if (isOpen(channel)) {
                    closeChannels(Collections.singletonList(channel));
                }
            } catch (IOException e) {
                log.warn("failed to close channel", e);
            } finally {
                outer:
                {
                    for (Map.Entry<DiscoveryNode, NodeChannel> entry : connectedNodes.entrySet()) {
                        if (disconnectFromNode(entry.getKey(), channel, reason)) {
                            // if we managed to find this channel and disconnect from it, then break, no need to check on
                            // the rest of the nodes
                            // #onNodeChannelsClosed will remove it..
                            assert openConnections.contains(entry.getValue()) == false : "NodeChannel#close should remove the connetion";
                            // we can only be connected and published to a single node with one connection. So if disconnectFromNode
                            // returns true we can safely break out from here since we cleaned up everything needed
                            break outer;
                        }
                    }
                    // now if we haven't found the right connection in the connected nodes we have to go through the open connections
                    // it might be that the channel belongs to a connection that is not published
                    for (NodeChannel channels : openConnections) {
                        if (channels.isChannel(channel)) {
                            IOUtils.closeWhileHandlingException(channels);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public Connection getConnection(DiscoveryNode node) {
        Connection nodeChannels = connectedNodes.get(node);
        if (nodeChannels == null) {
            throw new RuntimeException("Node not connected");
        }
        return nodeChannels;
    }

    /**
     * Ensures this transport is still started / open
     *
     * @throws IllegalStateException if the transport is not started / open
     */
    protected final void ensureOpen() {
        if (lifecycle.started() == false) {
            throw new IllegalStateException("transport has been stopped");
        }
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() {
        final CountDownLatch latch = new CountDownLatch(1);
        // make sure we run it on another thread than a possible IO handler thread
        new Thread(() -> {
            globalLock.writeLock().lock();
            try {
                // first stop to accept any incoming connections so nobody can connect to this transport
                for (Map.Entry<String, List<Channel>> entry : serverChannels.entrySet()) {
                    try {
                        closeChannels(entry.getValue());
                    } catch (Exception e) {
                        log.debug("Error closing serverChannel for profile [{}]", entry.getKey(), e);
                    }
                }
                // we are holding a write lock so nobody modifies the connectedNodes / openConnections map - it's safe to first close
                // all instances and then clear them maps
//                IOUtils.closeWhileHandlingException(Iterables.concat(connectedNodes.values(), openConnections));
                openConnections.clear();
                connectedNodes.clear();
                stopInternal();
            } finally {
                globalLock.writeLock().unlock();
                latch.countDown();
            }
        }).start();

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore
        }
    }

    @Override
    protected void doClose() throws IOException {

    }

    /**
     * Closes all channels in this list
     */
    protected abstract void closeChannels(List<Channel> channel) throws IOException;

    protected abstract InetSocketAddress getLocalAddress(Channel channel);

    protected abstract Channel bind(String name, InetSocketAddress address) throws IOException;

    protected abstract void bindServer(final String name, final Settings settings) throws IOException;

    /**
     * Called to tear down internal resources
     */
    protected abstract void stopInternal();

    protected InetSocketAddress bindToPort(final String name, final InetAddress hostAddress, String port) {
        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference<>();
        final AtomicReference<InetSocketAddress> boundSocket = new AtomicReference<>();
        boolean success = portsRange.iterate(portNumber -> {
            try {
                Channel channel = bind(name, new InetSocketAddress(hostAddress, portNumber));
                synchronized (serverChannels) {
                    List<Channel> list = serverChannels.get(name);
                    if (list == null) {
                        list = new ArrayList<>();
                        serverChannels.put(name, list);
                    }
                    list.add(channel);
                    boundSocket.set(getLocalAddress(channel));
                }
            } catch (Exception e) {
                lastException.set(e);
                return false;
            }
            return true;
        });
        if (!success) {
            throw new RuntimeException("Failed to bind to [" + port + "]", lastException.get());
        }

        return boundSocket.get();
    }

    private static final class VersionHandshakeResponse extends RpcResponse {

        private VersionHandshakeResponse() {
        }
    }

    private static class HandshakeResponseHandler<Channel> implements TransportResponseHandler<VersionHandshakeResponse> {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean handshakeNotSupported = new AtomicBoolean(false);
        final AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        final Channel channel;

        HandshakeResponseHandler(Channel channel) {
            this.channel = channel;
        }

        @Override
        public VersionHandshakeResponse newInstance() {
            return new VersionHandshakeResponse();
        }

        @Override
        public void handleResponse(VersionHandshakeResponse response) {
            latch.countDown();
        }

        @Override
        public void handleException(TransportException exp) {
            Throwable cause = exp.getCause();
            if (cause != null
//                    && cause instanceof ActionNotFoundTransportException
                    // this will happen if we talk to a node (pre 5.2) that doesn't have a handshake handler
                    // we will just treat the node as a 5.0.0 node unless the discovery node that is used to connect has a higher version.
                    && cause.getMessage().equals("No handler for action [internal:tcp/handshake]")) {
                handshakeNotSupported.set(true);
            } else {
                final boolean success = exceptionRef.compareAndSet(null, exp);
                assert success;
            }
            latch.countDown();
        }

    }

    public final class NodeChannel implements Connection {
        private final Channel channel;
        private final DiscoveryNode node;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        public NodeChannel(DiscoveryNode node, Channel channel) {
            this.node = node;
            this.channel = channel;
        }

        public boolean isChannel(Channel channel1) {
            if (channel.equals(channel1)) {
                return true;
            }
            return false;
        }

        public Channel getChannel() {
            return channel;
        }

        @Override
        public synchronized void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                try {
                    closeChannels(Lists.newArrayList(channel));
                } finally {
                    onNodeChannelsClosed(this);
                }
            }
        }

        @Override
        public DiscoveryNode getNode() {
            return this.node;
        }

        @Override
        public void sendRequest(RpcRequest request)
                throws Exception {
            if (closed.get()) {
                throw new RuntimeException("connection already closed");
            }
            Runnable onRequestSent = new Runnable() {
                @Override
                public void run() {
                    //todo
                }
            };
            sendMessage(channel, request, onRequestSent);
        }

    }

    protected abstract void sendMessage(Channel channel, RpcMessage message, Runnable sendListener) throws IOException;


    private void onNodeChannelsClosed(NodeChannel channel) {
        // don't assert here since the channel / connection might not have been registered yet
        final boolean remove = openConnections.remove(channel);
        if (remove) {
            transportServiceAdapter.onConnectionClosed(channel);
        }
    }
}
