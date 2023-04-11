// DefaultChannelPipeline内部类
// 持有unsafe对象，inbound的起点，outbound的终点，IO读写都需要经由它转交给unfafe处理
class HeadContext extends AbstractChannelHandlerContext implements ChannelOutboundHandler{
    private final Unsafe unsafe;

    HeadContext(DefaultChannelPipeline pipeline) {
        super(pipeline, null, HEAD_NAME, false, true);
        unsafe = pipeline.channel().unsafe();
        setAddComplete();
    }

    public void bind(
            ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
            throws Exception {
        unsafe.bind(localAddress, promise);
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();

        readIfIsAutoRead();
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        unsafe.write(msg, promise);
    }

}