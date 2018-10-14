package the.flash.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import the.flash.client.handler.FirstClientHandler;
import the.flash.client.handler.LoginResponseHandler;
import the.flash.client.handler.MessageResponseHandler;
import the.flash.codec.PacketDecoder;
import the.flash.codec.PacketEncoder;
import the.flash.codec.Spliter;
import the.flash.protocol.request.MessageRequestPacket;
import the.flash.util.LoginUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author 闪电侠
 */
public class NettyClient {
    private static final int MAX_RETRY = 5;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;
    public static final int CLIENT_COUNT = 2;
    private static List<Channel> channelList = new ArrayList();


    public static void main(String[] args) {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
//                        ch.pipeline().addLast(new FirstClientHandler());
                        ch.pipeline().addLast(new FirstClientHandler());

                    }
                });

        connect(bootstrap);
        //startChat();
    }

    private static void startChat() {
        Thread chatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if (channelList.size() != CLIENT_COUNT){
                        System.out.println("client is not ready. wait for 1s");
                        try {
                            Thread.sleep(10000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    int index = 0;
                    int clientIndex = index++ % CLIENT_COUNT;
                    Channel channel = channelList.get(clientIndex);
                    channel.writeAndFlush(new MessageRequestPacket("client-" + clientIndex + " sent"));
                }
            }
        });
    }

    private static void connect(Bootstrap bootstrap) {
        for (int i = 0; i < CLIENT_COUNT; i++) {
            bootstrap.connect(HOST, PORT).addListener(future -> {
                if (future.isSuccess()) {
//                System.out.println(new Date() + ": 连接成功，启动控制台线程……");
                    Channel channel = ((ChannelFuture) future).channel();
                    channelList.add(channel);
                    //startConsoleThread(channel);
                } else {
                    System.out.println("connect failed");
                }
            });
        }

    }

    private static void startConsoleThread(Channel channel) {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                if (LoginUtil.hasLogin(channel)) {
                    System.out.println("输入消息发送至服务端: ");
                    Scanner sc = new Scanner(System.in);
                    String line = sc.nextLine();

                    for (int i = 0; i < 1000; i++) {
                        channel.writeAndFlush(new MessageRequestPacket(line));
                    }
                }
            }
        }).start();
    }
}
