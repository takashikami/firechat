/* -*- tab-width:4 -*-
   Created on 2018/07/02 by kami
 */

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.util.CacheListenerAdapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class ChatServer extends CacheListenerAdapter<String, String> {
    private static final int BUF_SIZE = 65535;

    private static Selector selector;
    private static Region<String, String> region;

    public static void main(String[] args) {
        int portno = 7000;
        int locator = 10334;

        if (args.length == 1) {
            int delta = Integer.parseInt(args[0]);
            portno += delta;
            locator += delta;
        }
        Properties props = new Properties();
        props.setProperty("start-locator", "localhost["+locator+"]");
        CacheFactory factory = new CacheFactory(props);
        Cache cache = factory.create();
        region = cache.getRegion("ChatMessage");

        ServerSocketChannel serverChannel = null;
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(portno));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NonBlockingChannelEchoServerが起動しました(port="
                    + serverChannel.socket().getLocalPort() + ")");
            while (selector.select() > 0) {
                for (Iterator it = selector.selectedKeys().iterator(); it.hasNext();) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        doAccept((ServerSocketChannel) key.channel());
                    } else if (key.isReadable()) {
                        doRead((SocketChannel) key.channel());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverChannel != null && serverChannel.isOpen()) {
                try {
                    System.out.println("NonBlockingChannelEchoServerを停止します。");
                    serverChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static HashMap<Integer, SocketChannel> sockets = new HashMap<>();

    private static void doAccept(ServerSocketChannel serverChannel) {
        try {
            SocketChannel channel = serverChannel.accept();
            String mesg = "[接続しました]";
            String addr = channel.getRemoteAddress()+":";
            System.out.println(addr+mesg);
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            sockets.put(channel.socket().getPort(), channel);
            region.put(addr, addr+mesg+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void doRead(SocketChannel channel) {
        ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
        try {
            if (channel.read(buf) < 0) {
                String mesg = "[切断しました]";
                String addr = channel.getRemoteAddress()+":";
                System.out.println(addr+mesg);
                region.put(addr,addr+mesg+"\n");
                sockets.remove(channel.socket().getPort());
                channel.close();
            } else {
                buf.flip();
                String addr = channel.getRemoteAddress()+":";
                byte[] msgbin = new byte[buf.limit()];
                buf.get(msgbin);
                String mesg = new String(msgbin);
                System.out.print(addr+mesg);
                region.put(addr, addr+mesg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String str) {
        for (HashMap.Entry<Integer, SocketChannel> entry : sockets.entrySet()) {
            SocketChannel out = entry.getValue();
            try {
                out.write(ByteBuffer.wrap((str).getBytes()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterCreate(EntryEvent<String, String> event) {
        super.afterCreate(event);
        //System.out.println(event);
        broadcast(event.getNewValue());
    }

    @Override
    public void afterUpdate(EntryEvent<String, String> event) {
        super.afterUpdate(event);
        //System.out.println(event);
        broadcast(event.getNewValue());
    }

    @Override
    public void afterDestroy(EntryEvent<String, String> event) {
        super.afterDestroy(event);
        //System.out.println(event);
    }
}
