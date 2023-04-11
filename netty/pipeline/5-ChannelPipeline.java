基于版本：4.1.25.Final
ChannelPipeline相关代码


interface ChannelPipeline{
    // 对于NioServerSocketChannel，当建立新连接时，会调用该方法，msg为NioSocketChannel
    ChannelPipeline fireChannelRead(Object msg);

    // 调用fireChannelRead(msg)之后调用该方法
    ChannelPipeline fireChannelReadComplete();
}

class DefaultChannelPipeline{

	final AbstractChannelHandlerContext tail;

	protected DefaultChannelPipeline(Channel channel) {
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);

        tail = new TailContext(this);
        head = new HeadContext(this);

        head.next = tail;
        tail.prev = head;
    }

	public final ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return tail.bind(localAddress, promise);
    }

    public final ChannelPipeline fireChannelActive() {
        AbstractChannelHandlerContext.invokeChannelActive(head);
        return this;
    }





    //
    // 增加ChannelHandler
    //
    public final ChannelPipeline addFirst(EventExecutorGroup group{default:null}, String name, ChannelHandler handler) {
        final AbstractChannelHandlerContext newCtx;
        synchronized (this) {
            。。。
            name = filterName(name, handler);

            newCtx = newContext(group, name, handler);  //new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);

            addFirst0(newCtx);

            。。。

            EventExecutor executor = newCtx.executor();
            if (!executor.inEventLoop()) {
                newCtx.setAddPending();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callHandlerAdded0(newCtx);
                    }
                });
                return this;
            }
        }
        callHandlerAdded0(newCtx);  //触发newCtx的ChannelHandler#handlerAdded(ctx)
        return this;
    }
    private void addFirst0(AbstractChannelHandlerContext newCtx) {
        AbstractChannelHandlerContext nextCtx = head.next;
        newCtx.prev = head;
        newCtx.next = nextCtx;
        head.next = newCtx;
        nextCtx.prev = newCtx;
    }
    public final ChannelPipeline addLast(String name, ChannelHandler handler) {
        return addLast(null, name, handler);
    }
    public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        final AbstractChannelHandlerContext newCtx;
        ..
        checkMultiplicity(handler);

        newCtx = newContext(group, filterName(name, handler), handler); //newCtx = new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);

        addLast0(newCtx);

        // If the registered is false it means that the channel was not registered on an eventloop yet.
        // In this case we add the context to the pipeline and add a task that will call
        // ChannelHandler.handlerAdded(...) once the channel is registered.
        if (!registered) {
            newCtx.setAddPending();
            callHandlerCallbackLater(newCtx, true);
            return this;
        }

        EventExecutor executor = newCtx.executor();
        if (!executor.inEventLoop()) {
            newCtx.setAddPending();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callHandlerAdded0(newCtx);
                }
            });
            return this;
        }
        ..
        callHandlerAdded0(newCtx);
        return this;
    }
    private EventExecutor childExecutor(EventExecutorGroup group) {
        if (group == null) {
            return null;
        }
        ..
        return childExecutor;
    }







    //
    // 数据入站相关
    //

    public final ChannelPipeline fireChannelRead(Object msg) {
        AbstractChannelHandlerContext.invokeChannelRead(head, msg);     //最终会调用到ChannelHandlerContext#invokeChannelRead(msg)
        return this;
    }


}




















