/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.util.CacheListenerAdapter;

import java.util.Properties;

/**
 * Simple chat server modified from SecureChatServer.
 */
public final class FireChatServer extends CacheListenerAdapter<String, String> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    static final String LOCATOR_HOST = System.getProperty("locatorHost", "localhost");
    static final int LOCATOR_PORT = Integer.parseInt(System.getProperty("locatorPort", "10334"));
    static final int PORT = Integer.parseInt(System.getProperty("port", "7000"));

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("start-locator", LOCATOR_HOST+"["+LOCATOR_PORT+"]");
        System.out.println(props);

        CacheFactory factory = new CacheFactory(props);
        Cache cache = factory.create();
        Region<String, String> region = cache.getRegion("FireChatMessage");

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline pipeline = ch.pipeline();

                     // On top of the SSL handler, add the text line codec.
                     pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                     pipeline.addLast(new StringDecoder());
                     pipeline.addLast(new StringEncoder());

                     // and then business logic.
                     pipeline.addLast(new FireChatServerHandler(region, channels));
                 }
             });

            // Start the server.
            System.out.println("start listening "+PORT);
            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("server started "+PORT);

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void broadcast(String addrmsg) {
        for (Channel c: channels) {
            c.writeAndFlush(addrmsg + "\n");
        }
    }

    @Override
    public void afterCreate(EntryEvent<String, String> event) {
        System.out.println(event);
        broadcast(event.getKey()+event.getNewValue());
    }

    @Override
    public void afterUpdate(EntryEvent<String, String> event) {
        System.out.println(event);
        broadcast(event.getKey()+event.getNewValue());
    }

    @Override
    public void afterDestroy(EntryEvent<String, String> event) {
        System.out.println(event);
    }
}
