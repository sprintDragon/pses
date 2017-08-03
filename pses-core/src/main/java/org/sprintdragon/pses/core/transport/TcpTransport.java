package org.sprintdragon.pses.core.transport;

import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.common.network.NetworkService;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.common.util.concurrent.KeyedLock;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by wangdi on 17-8-2.
 */
@Slf4j
public abstract class TcpTransport extends AbstractLifecycleComponent implements Transport {

    @Resource
    protected NetworkService networkService;
    protected final Map<String, List<Channel>> serverChannels = new ConcurrentHashMap<>();
    protected volatile BoundTransportAddress boundAddress;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    protected volatile TransportServiceAdapter transportServiceAdapter;
    // node id to actual channel
    protected final ConcurrentMap<DiscoveryNode, Channel> connectedNodes = new ConcurrentHashMap<>();
    private final Set<Channel> openConnections = new ConcurrentSet<>();
    protected final KeyedLock<String> connectionLock = new KeyedLock<>();

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

    protected abstract Channel connectToChannel(DiscoveryNode node);

    @Override
    public void connectToNode(DiscoveryNode node) throws Exception {
        ensureOpen();
        if (node == null) {
            throw new RuntimeException("can't connect to a null node");
        }
        globalLock.readLock().lock();
        try {
            connectionLock.acquire(node.id());
            try {
                ensureOpen();
                Channel nodeChannel = connectedNodes.get(node);
                if (nodeChannel != null) {
                    return;
                }
                try {
                    nodeChannel = connectToChannel(node);
                    // we acquire a connection lock, so no way there is an existing connection
                    connectedNodes.put(node, nodeChannel);
                    if (log.isDebugEnabled()) {
                        log.debug("connected to node [{}]", node);
                    }
                    transportServiceAdapter.raiseNodeConnected(node);
                } catch (Exception e) {
                    throw new RuntimeException("general node connection failure", e);
                }
            } finally {
                connectionLock.release(node.id());
            }
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public void disconnectFromNode(DiscoveryNode node) {
        connectionLock.acquire(node.id());
        try {
            Channel nodeChannels = connectedNodes.remove(node);
            if (nodeChannels != null) {
                try {
                    log.debug("disconnecting from [{}] due to explicit disconnect call", node);
                    nodeChannels.close();
                } finally {
                    log.trace("disconnected from [{}] due to explicit disconnect call", node);
                    transportServiceAdapter.raiseNodeDisconnected(node);
                }
            }
        } finally {
            connectionLock.release(node.id());
        }
    }

    public Channel nodeChannel(DiscoveryNode node) throws Exception {
        Channel nodeChannels = connectedNodes.get(node);
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

    public abstract void sendResponse(Channel channel, RpcResponse response);

}
