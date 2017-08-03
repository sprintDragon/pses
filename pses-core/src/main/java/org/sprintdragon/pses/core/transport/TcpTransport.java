package org.sprintdragon.pses.core.transport;

import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.action.ActionFuture;
import org.sprintdragon.pses.core.cluster.node.DiscoveryNode;
import org.sprintdragon.pses.core.common.component.AbstractLifecycleComponent;
import org.sprintdragon.pses.core.common.network.NetworkService;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
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
public abstract class TcpTransport<Channel> extends AbstractLifecycleComponent {

    @Resource
    protected NetworkService networkService;
    protected final Map<String, List<Channel>> serverChannels = new ConcurrentHashMap<>();
    protected volatile BoundTransportAddress boundAddress;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    // node id to actual channel
    protected final ConcurrentMap<DiscoveryNode, Channel> connectedNodes = new ConcurrentHashMap<>();
    private final Set<Channel> openConnections = new ConcurrentSet<>();

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

    public abstract void sendResponse(Channel channel, RpcResponse response, Long requestId, String action);

    public abstract void sendErrorResponse(Channel channel, Exception exception, Long requestId, String action);

    public abstract ActionFuture<RpcResponse> sendRequest(Channel channel, RpcRequest request);
}
