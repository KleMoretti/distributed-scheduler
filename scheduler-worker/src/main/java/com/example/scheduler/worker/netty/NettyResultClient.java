package com.example.scheduler.worker.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class NettyResultClient {

    private static final Logger log = LoggerFactory.getLogger(NettyResultClient.class);

    @Value("${scheduler.callback.host:127.0.0.1}")
    private String host;

    @Value("${scheduler.callback.port:19090}")
    private int port;

    // Shared across all sendResult() calls — avoids spawning new threads per callback
    private EventLoopGroup group;

    @PostConstruct
    public void init() {
        group = new NioEventLoopGroup(1);
    }

    @PreDestroy
    public void destroy() {
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public void sendResult(Long jobId, String workerId, int status, String message) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                    }
                });

            Channel channel = bootstrap.connect(host, port).sync().channel();
            String payload = String.format("jobId=%d,worker=%s,status=%d,message=%s", jobId, workerId, status, message);
            // Write String directly so StringEncoder converts it — no manual ByteBuf wrapping needed
            channel.writeAndFlush(payload).sync();
            channel.close().sync();
        } catch (Exception ex) {
            log.debug("Send netty callback failed, ignore for now", ex);
        }
    }
}
