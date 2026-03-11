package com.example.scheduler.core.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class NettyResultServer {

    private static final Logger log = LoggerFactory.getLogger(NettyResultServer.class);

    @Value("${scheduler.callback.host:0.0.0.0}")
    private String host;

    @Value("${scheduler.callback.port:19090}")
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture bindFuture;

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                if (msg instanceof ByteBuf byteBuf) {
                                    String payload = byteBuf.toString(StandardCharsets.UTF_8).trim();
                                    if (!payload.isBlank()) {
                                        log.info("Receive worker callback: {}", payload);
                                    }
                                    byteBuf.release();
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.warn("Netty callback connection error", cause);
                                ctx.close();
                            }
                        });
                    }
                });

            bindFuture = bootstrap.bind(host, port).sync();
            log.info("Netty callback server started on {}:{}", host, port);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Start netty callback server interrupted", ex);
        } catch (Exception ex) {
            shutdown();
            throw new IllegalStateException("Start netty callback server failed", ex);
        }
    }

    @PreDestroy
    public void stop() {
        shutdown();
    }

    private void shutdown() {
        if (bindFuture != null && bindFuture.channel() != null) {
            bindFuture.channel().close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
