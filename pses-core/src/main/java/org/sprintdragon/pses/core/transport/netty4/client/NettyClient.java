package org.sprintdragon.pses.core.transport.netty4.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sprintdragon.pses.core.action.ActionFuture;
import org.sprintdragon.pses.core.action.ActionListenerResponseHandler;
import org.sprintdragon.pses.core.action.supprot.PlainActionFuture;
import org.sprintdragon.pses.core.transport.TransportService;
import org.sprintdragon.pses.core.transport.dto.RpcRequest;
import org.sprintdragon.pses.core.transport.dto.RpcResponse;
import org.sprintdragon.pses.core.transport.netty4.ClientRpcHandler;
import org.sprintdragon.pses.core.transport.netty4.codec.RpcDecoder;
import org.sprintdragon.pses.core.transport.netty4.codec.RpcEncoder;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * https://github.com/dozer47528/AutoReconnectNettyExample/blob/master/src/main/java/cc/dozer/netty/example/TcpClient.java
 */
public class NettyClient implements IClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    @Resource
    TransportService transportService;
    private EventLoopGroup workerGroup;
    private Bootstrap bootstrap;
    private Channel channel;

    private ClientRpcHandler clientRpcHandler = new ClientRpcHandler();

    private volatile boolean closed = false;

    //    @Value("${client.workerGroupThreads:5}")
    int workerGroupThreads = 5;

    public void connect(final InetSocketAddress socketAddress) {
        try {
            workerGroup = new NioEventLoopGroup(workerGroupThreads);
            bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
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
                                                    doConnect(socketAddress);
                                                }
                                            }, 1, TimeUnit.SECONDS);
                                        }
                                    })
                                    //处理分包传输问题
                                    .addLast("decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                                    .addLast("encoder", new LengthFieldPrepender(4, false))
                                    .addLast(new RpcDecoder(RpcResponse.class))
                                    .addLast(new RpcEncoder(RpcRequest.class))
                                    .addLast(clientRpcHandler);
                        }
                    });
            doConnect(socketAddress);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void doConnect(final InetSocketAddress socketAddress) {
        logger.info("trying to connect server:{}", socketAddress);
        if (closed) {
            return;
        }

        ChannelFuture future = bootstrap.connect(socketAddress);
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()) {
                    logger.info("connected to {}", socketAddress);
                } else {
                    logger.info("connected to {} failed", socketAddress);
                    f.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect(socketAddress);
                        }
                    }, 1, TimeUnit.SECONDS);
                }
            }
        });

        channel = future.syncUninterruptibly()
                .channel();
    }

    public ActionFuture<RpcResponse> send(RpcRequest request) throws InterruptedException {
        System.out.println("send request:" + request);
        PlainActionFuture<RpcResponse> actionFuture = PlainActionFuture.newFuture();
        execute(request, actionFuture);
        return actionFuture;
    }

    private void execute(RpcRequest request, PlainActionFuture<RpcResponse> actionFuture) {
        transportService.sendRequest(channel, request, new ActionListenerResponseHandler<RpcResponse>(actionFuture) {
            @Override
            public RpcResponse newInstance() {
                return new RpcResponse();
            }
        });
    }

//    public void asyncSend(RpcRequest request, Pair<Long, TimeUnit> timeout, ActionListener<RpcResponse> listener) throws InterruptedException {
//        ChannelFuture future = channel.writeAndFlush(request);
//        future.addListener(f -> sendListener.run());
//    }

    public InetSocketAddress getRemoteAddress() {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (!(remoteAddress instanceof InetSocketAddress)) {
            throw new RuntimeException("Get remote address error, should be InetSocketAddress");
        }
        return (InetSocketAddress) remoteAddress;
    }

    public boolean isClosed() {
        return closed;
    }

    @PreDestroy
    public void close() {
        logger.info("destroy client resources");
        if (null == channel) {
            logger.error("channel is null");
        }
        closed = true;
        workerGroup.shutdownGracefully();
        channel.closeFuture().syncUninterruptibly();
        workerGroup = null;
        channel = null;
    }
}
