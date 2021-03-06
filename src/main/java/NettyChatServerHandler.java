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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Handles a server-side channel.
 */
public class NettyChatServerHandler extends SimpleChannelInboundHandler<String> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        channels.add(ctx.channel());

        String outmsg = ctx.channel().remoteAddress() + ":[接続しました]\n";
        for (Channel c: channels) {
            c.writeAndFlush(outmsg);
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        String outmsg = ctx.channel().remoteAddress() + ":" + msg + "\n";
        for (Channel c: channels) {
            c.writeAndFlush(outmsg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String outmsg = ctx.channel().remoteAddress() + ":[切断しました]\n";
        for (Channel c: channels) {
            c.writeAndFlush(outmsg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
