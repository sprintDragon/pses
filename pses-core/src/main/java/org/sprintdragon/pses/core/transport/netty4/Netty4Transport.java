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
import org.apache.commons.lang3.StringUtils;
import org.sprintdragon.pses.core.common.network.NetworkService;
import org.sprintdragon.pses.core.common.settings.Settings;
import org.sprintdragon.pses.core.transport.TcpTransport;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.codec.RpcDecoder;
import org.sprintdragon.pses.core.transport.netty4.codec.RpcEncoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangdi on 17-8-2.
 */
public class Netty4Transport extends TcpTransport<Channel> {

    // package private for testing
    volatile Netty4OpenChannelsHandler serverOpenChannels;
    protected volatile Bootstrap bootstrap;
    protected final Map<String, ServerBootstrap> serverBootstraps = new ConcurrentHashMap<>();

    public Netty4Transport(Settings settings, NetworkService networkService) {
        super(settings, networkService);
    }

    @Override
    protected void doStart() {
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
                        .addLast(new ClientRpcHandler());
            }
        });
//        final ByteSizeValue tcpSendBufferSize = TCP_SEND_BUFFER_SIZE.get(settings);
//        if (tcpSendBufferSize.getBytes() > 0) {
//            bootstrap.option(ChannelOption.SO_SNDBUF, Math.toIntExact(tcpSendBufferSize.getBytes()));
//        }
//
//        final ByteSizeValue tcpReceiveBufferSize = TCP_RECEIVE_BUFFER_SIZE.get(settings);
//        if (tcpReceiveBufferSize.getBytes() > 0) {
//            bootstrap.option(ChannelOption.SO_RCVBUF, Math.toIntExact(tcpReceiveBufferSize.getBytes()));
//        }
//
//        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, recvByteBufAllocator);

//        final boolean reuseAddress = TCP_REUSE_ADDRESS.get(settings);
//        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);

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
                        .addLast(new ServerRpcHandler());
            }
        });

        serverBootstrap.validate();

        serverBootstraps.put(name, serverBootstrap);
    }

    protected InetSocketAddress getLocalAddress(Channel channel) {
        return (InetSocketAddress) channel.localAddress();
    }

    protected Channel bind(String name, InetSocketAddress address) {
        return serverBootstraps.get(name).bind(address).syncUninterruptibly().channel();
    }

    protected void bindServer(final String name, final Settings settings) {
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

        List<InetSocketAddress> boundAddresses = new ArrayList<>();
        for (InetAddress hostAddress : hostAddresses) {
            boundAddresses.add(bindToPort(name, hostAddress, settings.get("port")));
        }

        this.boundAddresses = boundAddresses;
    }

    @Override
    public void sendResponse(Channel channel, RpcResponse response, long requestId, String action) {
        channel.writeAndFlush(response);
    }

    @Override
    public void sendErrorResponse(Channel channel, Exception exception, long requestId, String action) {
        channel.writeAndFlush(exception);
    }

    @Override
    public void sendRequest(Channel channel, long requestId, RpcRequest request) {
        channel.writeAndFlush(request);
    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() throws IOException {

    }
}
