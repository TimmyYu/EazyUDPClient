package com.qq;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SocketUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by huitaoyu on 19/12/2017.
 */
public class UDPClient {
    static final int PORT = Integer.parseInt(System.getProperty("port", "7686"));
    public static final Map<Channel, Header>  channelMap = new ConcurrentHashMap<Channel, Header>();
    private Bootstrap bootstrap;

    public UDPClient() {
        EventLoopGroup group = new KQueueEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(KQueueDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new QuoteOfTheMomentClientHandler());

    }


    public CompletableFuture<byte[]> executeRequest(byte[] requestByte, InetSocketAddress address) throws InterruptedException {
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<byte[]>();
        Channel ch = bootstrap.bind(0).sync().channel();
        channelMap.put(ch, result -> {
            completableFuture.complete(result);

        });
        // Broadcast the QOTM request to port 8080.
        ChannelFuture future = ch.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(requestByte),
                address)).sync();
        return completableFuture;
    }


    public static void main(String[] args) throws Exception {
        UDPClient client = new UDPClient();
        for(int i = 0 ; i<10000;i++) {
            long start = System.nanoTime();
            CompletableFuture<byte[]> com = client.executeRequest("测试新的应用".getBytes(), SocketUtils.socketAddress("127.0.0.1", PORT))
                    .thenApplyAsync(x->{
                        long end = (System.nanoTime()-start)/1000;
                        System.out.println(new String(x)+end);
                        return null;
                    });
        }
    }

}
