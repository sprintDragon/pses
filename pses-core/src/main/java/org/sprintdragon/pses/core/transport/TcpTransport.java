package org.sprintdragon.pses.core.transport;

import lombok.extern.slf4j.Slf4j;
import org.sprintdragon.pses.core.action.ActionFuture;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by wangdi on 17-8-2.
 */
@Slf4j
public abstract class TcpTransport<Channel> extends AbstractLifecycleComponent {

    @Resource
    protected NetworkService networkService;
    protected final Map<String, List<Channel>> serverChannels = new ConcurrentHashMap<>();
    protected volatile List<InetSocketAddress> boundAddresses;

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() throws IOException {

    }

    protected abstract InetSocketAddress getLocalAddress(Channel channel);

    protected abstract Channel bind(String name, InetSocketAddress address) throws IOException;

    protected abstract void bindServer(final String name, final Settings settings);

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

    public abstract void sendResponse(Channel channel, RpcResponse response, long requestId, String action);

    public abstract void sendErrorResponse(Channel channel, Exception exception, long requestId, String action);

    public abstract ActionFuture<RpcResponse> sendRequest(Channel channel, long requestId, RpcRequest request);
}
