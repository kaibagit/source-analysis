// DefaultChannelPipeline内部类
class TailContext extends AbstractChannelHandlerContext implements ChannelInboundHandler{

    Unsafe unsafe;

    // AbstractChannelHandlerContext
    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        final AbstractChannelHandlerContext next = findContextOutbound();   //找到HeadContext
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeBind(localAddress, promise);
        } else {
            safeExecute(executor, new Runnable() {
                @Override
                public void run() {
                    next.invokeBind(localAddress, promise);
                }
            }, promise, null);
        }
        return promise;
    }
    // AbstractChannelHandlerContext
    private void invokeBind(SocketAddress localAddress, ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).bind(this, localAddress, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            bind(localAddress, promise);
        }
    }
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        unsafe.write(msg, promise);
    }
}