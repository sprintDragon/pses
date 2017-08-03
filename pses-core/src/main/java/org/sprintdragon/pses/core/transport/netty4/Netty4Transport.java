package org.sprintdragon.pses.core.transport.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.action.ActionFuture;
import org.sprintdragon.pses.core.action.ActionListenerResponseHandler;
import org.sprintdragon.pses.core.action.supprot.PlainActionFuture;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.transport.BoundTransportAddress;
import org.sprintdragon.pses.core.transport.TcpTransport;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.codec.RpcDecoder;
import org.sprintdragon.pses.core.transport.netty4.codec.RpcEncoder;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by wangdi on 17-8-2.
 */
@Slf4j
@Component
public class Netty4Transport extends TcpTransport<Channel> {

    @Resource
    ClientRpcHandler clientRpcHandler;
    @Resource
    ServerRpcHandler serverRpcHandler;
    // package private for testing
    volatile Netty4OpenChannelsHandler serverOpenChannels;
    protected volatile Bootstrap bootstrap;
    protected final Map<String, ServerBootstrap> serverBootstraps = new ConcurrentHashMap<>();
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    @Resource
    TransportService transportService;

    @Override
    protected void doStart() throws Exception {
        boolean success = false;
        try {
            bootstrap = createBootstrap();
            final Netty4OpenChannelsHandler openChannels = new Netty4OpenChannelsHandler();
            this.serverOpenChannels = openChannels;
            String name = "default";
            createServerBootstrap(name, settings);
            bindServer(name, settings);
            super.doStart();
            success = true;
        } finally {
            if (success == false) {
                doStop();
            }
        }
    }


    /**
     * client bootstrap
     *
     * @return
     */
    private Bootstrap createBootstrap() {
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(5));
        bootstrap.channel(NioSocketChannel.class);


//        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(defaultConnectionProfile.getConnectTimeout().millis()));
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        //处理失败重连
                        .addFirst(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                ctx.channel().eventLoop().schedule(new Runnable() {
                                    @Override
                                    public void run() {
//                                        doConnect(socketAddress);
                                    }
                                }, 1, TimeUnit.SECONDS);
                            }
                        })
//                                    //处理分包传输问题
                        .addLast("decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                        .addLast("encoder", new LengthFieldPrepender(4, false))
                        .addLast(new RpcDecoder(RpcResponse.class))
                        .addLast(new RpcEncoder(RpcRequest.class))
                        .addLast(clientRpcHandler);
            }
        });
        bootstrap.validate();

        return bootstrap;
    }

    /**
     * server biitstrap
     */
    private void createServerBootstrap(String name, Settings settings) {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(new NioEventLoopGroup(5));
        serverBootstrap.channel(NioServerSocketChannel.class);

        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        //处理失败重连
                        .addFirst(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                ctx.channel().eventLoop().schedule(new Runnable() {
                                    @Override
                                    public void run() {
//                                        doConnect(socketAddress);
                                    }
                                }, 1, TimeUnit.SECONDS);
                            }
                        })
//                                    //处理分包传输问题
                        .addLast("decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                        .addLast("encoder", new LengthFieldPrepender(4, false))
                        .addLast(new RpcDecoder(RpcRequest.class))
                        .addLast(new RpcEncoder(RpcResponse.class))
                        .addLast(serverRpcHandler);
            }
        });

        serverBootstrap.validate();

        serverBootstraps.put(name, serverBootstrap);
    }

    public Channel doConnect(final InetSocketAddress socketAddress) {
        log.info("trying to connect server:{}", socketAddress);
//        if (closed) {
//            return null;
//        }

        ChannelFuture future = bootstrap.connect(socketAddress);
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()) {
                    log.info("connected to {}", socketAddress);
                } else {
                    log.info("connected to {} failed", socketAddress);
                    f.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect(socketAddress);
                        }
                    }, 1, TimeUnit.SECONDS);
                }
            }
        });

        return future.syncUninterruptibly()
                .channel();
    }

    public ActionFuture<RpcResponse> send(Channel channel, RpcRequest request) throws InterruptedException {
        System.out.println("send request:" + request);
        PlainActionFuture<RpcResponse> actionFuture = PlainActionFuture.newFuture();
        execute(channel, request, actionFuture);
        return actionFuture;
    }

    private void execute(Channel channel, RpcRequest request, PlainActionFuture<RpcResponse> actionFuture) {
        transportService.sendRequest(channel, request, new ActionListenerResponseHandler<RpcResponse>(actionFuture) {
            @Override
            public RpcResponse newInstance() {
                return new RpcResponse();
            }
        });
    }


    protected InetSocketAddress getLocalAddress(Channel channel) {
        return (InetSocketAddress) channel.localAddress();
    }

    protected Channel bind(String name, InetSocketAddress address) {
        return serverBootstraps.get(name).bind(address).syncUninterruptibly().channel();
    }

    protected void bindServer(final String name, final Settings settings) throws IOException {
        // Bind and start to accept incoming connections.
        InetAddress hostAddresses[];
        String bindHostsStr = settings.get("bind_host");
        String[] bindHosts = StringUtils.isEmpty(bindHostsStr) ? null : bindHostsStr.split(",");
        try {
            hostAddresses = networkService.resolveBindHostAddresses(bindHosts);
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve host " + Arrays.toString(bindHosts) + "", e);
        }

        assert hostAddresses.length > 0;

        String port = settings.get("port");
        List<InetSocketAddress> boundAddresses = new ArrayList<>();
        for (InetAddress hostAddress : hostAddresses) {
            boundAddresses.add(bindToPort(name, hostAddress, port));
        }

        String networkHost = settings.get("network.host");
        InetSocketAddress publicAddress = createPublishAddress(networkHost, Integer.valueOf(port));
        this.boundAddress = new BoundTransportAddress(boundAddresses.toArray(new InetSocketAddress[0]), publicAddress);
    }

    private InetSocketAddress createPublishAddress(String publishHost, int publishPort) {
        try {
            return new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost), publishPort);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve publish address", e);
        }
    }

    @Override
    public void sendResponse(Channel channel, RpcResponse response, Long requestId, String action) {
        channel.writeAndFlush(response);
    }

    @Override
    public void sendErrorResponse(Channel channel, Exception exception, Long requestId, String action) {
        channel.writeAndFlush(exception);
    }

    @Override
    public ActionFuture<RpcResponse> sendRequest(Channel channel, RpcRequest request) {
        System.out.println("send request:" + request);
        PlainActionFuture<RpcResponse> actionFuture = PlainActionFuture.newFuture();
        execute(channel, request, actionFuture);
        return actionFuture;
    }

    @Override
    protected void closeChannels(final List<Channel> channels) throws IOException {
        Netty4Utils.closeChannels(channels);
    }

    @Override
    protected void stopInternal() {
        final CountDownLatch latch = new CountDownLatch(1);
        // make sure we run it on another thread than a possible IO handler thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                globalLock.writeLock().lock();
                try {

                    Iterator<Map.Entry<String, List<Channel>>> serverChannelIterator = serverChannels.entrySet().iterator();
                    while (serverChannelIterator.hasNext()) {
                        Map.Entry<String, List<Channel>> serverChannelEntry = serverChannelIterator.next();
                        String name = serverChannelEntry.getKey();
                        List<Channel> serverChannels = serverChannelEntry.getValue();
                        for (Channel serverChannel : serverChannels) {
                            try {
                                serverChannel.close().awaitUninterruptibly();
                            } catch (Throwable t) {
                                log.debug("Error closing serverChannel for profile [{}]", t, name);
                            }
                        }
                        serverChannelIterator.remove();
                    }

                    if (serverOpenChannels != null) {
                        serverOpenChannels.close();
                        serverOpenChannels = null;
                    }

                    Iterator<Map.Entry<String, ServerBootstrap>> serverBootstrapIterator = serverBootstraps.entrySet().iterator();
                    while (serverBootstrapIterator.hasNext()) {
                        Map.Entry<String, ServerBootstrap> serverBootstrapEntry = serverBootstrapIterator.next();
                        String name = serverBootstrapEntry.getKey();
                        ServerBootstrap serverBootstrap = serverBootstrapEntry.getValue();

                        try {
                            serverBootstrap.config().group().shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
                        } catch (Throwable t) {
                            log.debug("Error closing serverBootstrap for profile [{}]", t, name);
                        }

                        serverBootstrapIterator.remove();
                    }


                    if (bootstrap != null) {
                        bootstrap.config().group().shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
                        bootstrap = null;
                    }
                } finally {
                    globalLock.writeLock().unlock();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    protected void doClose() throws IOException {

    }
}
