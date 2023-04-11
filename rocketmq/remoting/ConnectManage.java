// NettyRemotingServer内部类
class NettyConnectManageHandler{

	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                // 触发超时，Server端直接关闭连接
                RemotingUtil.closeChannel(ctx.channel());
                if (NettyRemotingServer.this.channelEventListener != null) {
                    NettyRemotingServer.this
                        .putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                }
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

}


// NettyRemotingClient内部类
class NettyConnectManageHandler{

	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                // 触发超时，Client端直接关闭连接
                closeChannel(ctx.channel());
                if (NettyRemotingClient.this.channelEventListener != null) {
                    NettyRemotingClient.this
                        .putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                }
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

}