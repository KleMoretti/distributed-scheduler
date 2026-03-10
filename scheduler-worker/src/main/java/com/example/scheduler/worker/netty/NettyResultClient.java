package com.example.scheduler.worker.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
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

    public void sendResult(Long jobId, String workerId, int status, String message) {
        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                });

            Channel channel = bootstrap.connect(host, port).sync().channel();
            String payload = String.format("jobId=%d,worker=%s,status=%d,message=%s", jobId, workerId, status, message);
            channel.writeAndFlush(Unpooled.copiedBuffer(payload, StandardCharsets.UTF_8)).sync();
            channel.close().sync();
        } catch (Exception ex) {
            log.debug("Send netty callback failed, ignore for now", ex);
        } finally {
            group.shutdownGracefully();
        }
    }
}
