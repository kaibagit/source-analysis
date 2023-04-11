基于版本：4.1.25.Final
UNsafe相关代码

UNsafe
    |-NioUnsafe
    |--AbstractUnsafe
        |--AbstractNioUnsafe
            |--NioMessageUnsafe

// 继承关系：
NioSocketChannelUnsafe << NioByteUnsafe << AbstractNioUnsafe << AbstractUnsafe












class AbstractUnsafe{
	public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
        boolean wasActive = isActive();
        try {
            doBind(localAddress);	//调用了channel的doBind(SocketAddress localAddress)方法
        } catch (Throwable t) {
            safeSetFailure(promise, t);
            closeIfClosed();
            return;
        }

        if (!wasActive && isActive()) {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    pipeline.fireChannelActive();	//注册IO事件入口
                }
            });
        }

        safeSetSuccess(promise);
    }
}














// NioSocketChannel内部类
private class NioSocketChannelUnsafe extends NioByteUnsafe{

}

// AbstractNioByteChannel内部类
class NioByteUnsafe extends AbstractNioUnsafe{

    public final void read() {
        final ChannelConfig config = config();
        ..
        final ByteBufAllocator allocator = config.getAllocator();   //默认情况下：PooledByteBufAllocator(directByDefault: true)
        ..
        final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();   //默认情况下：AdaptiveRecvByteBufAllocator$HandlerImpl
        ..
        ByteBuf byteBuf = allocHandle.allocate(allocator);
        ..
        pipeline.fireChannelRead(byteBuf);
        ..
    }
}













//AbstractNioMessageChannel内部类
private final class NioMessageUnsafe extends AbstractNioUnsafe {

    private final List<Object> readBuf = new ArrayList<Object>();

    public void read() {
        ..
        final ChannelConfig config = config();
        final ChannelPipeline pipeline = pipeline();
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
        allocHandle.reset(config);

        boolean closed = false;
        Throwable exception = null;
        try {
            ..
            do {
                // 如果是NioServerSocketChannel，则调用ServerSocketChannel#accept()。建立连接之后，readBuf加入新创建的NioSocketChannel，并返回1，否则返回0
                int localRead = doReadMessages(readBuf);
                if (localRead == 0) {
                    break;
                }
                if (localRead < 0) {
                    closed = true;
                    break;
                }

                allocHandle.incMessagesRead(localRead);
            } while (allocHandle.continueReading());
            ..

            int size = readBuf.size();
            for (int i = 0; i < size; i ++) {
                readPending = false;
                pipeline.fireChannelRead(readBuf.get(i));
            }
            readBuf.clear();
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();

            if (exception != null) {
                closed = closeOnReadError(exception);

                pipeline.fireExceptionCaught(exception);
            }

            if (closed) {
                inputShutdown = true;
                if (isOpen()) {
                    close(voidPromise());
                }
            }
        } finally {
            // Check if there is a readPending which was not processed yet.
            // This could be for two reasons:
            // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
            // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
            //
            // See https://github.com/netty/netty/issues/2254
            if (!readPending && !config.isAutoRead()) {
                removeReadOp();
            }
        }
    }

    // AbstractUnsafe
    public final void register(EventLoop eventLoop, final ChannelPromise promise) {
    	// ...
        AbstractChannel.this.eventLoop = eventLoop;		//将eventLoop绑定到channel上

        // 保证register0(promise)在EventLoop中执行
        if (eventLoop.inEventLoop()) {
            register0(promise);
        } else {
            try {
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        register0(promise);
                    }
                });
            } catch (Throwable t) {
            	// ...
            }
        }
    }
    // AbstractUnsafe
    private void register0(ChannelPromise promise) {
        try {
            boolean firstRegistration = neverRegistered;
            doRegister();		//调用channel的doRegister()方法，获取SelectionKey对象
            neverRegistered = false;
            registered = true;

            // Ensure we call handlerAdded(...) before we actually notify the promise. This is needed as the
            // user may already fire events through the pipeline in the ChannelFutureListener.
            pipeline.invokeHandlerAddedIfNeeded();

            safeSetSuccess(promise);
            pipeline.fireChannelRegistered();
            // Only fire a channelActive if the channel has never been registered. This prevents firing
            // multiple channel actives if the channel is deregistered and re-registered.
            if (isActive()) {
                if (firstRegistration) {
                    pipeline.fireChannelActive();
                } else if (config().isAutoRead()) {
                    // This channel was registered before and autoRead() is set. This means we need to begin read
                    // again so that we process inbound data.
                    //
                    // See https://github.com/netty/netty/issues/4805
                    beginRead();
                }
            }
        } catch (Throwable t) {
        	// ...
        }
    }

    // AbstractNioUnsafe
    protected void doBeginRead() throws Exception {
        // Channel.read() or ChannelHandlerContext.read() was called
        final SelectionKey selectionKey = this.selectionKey;
        if (!selectionKey.isValid()) {
            return;
        }

        readPending = true;

        final int interestOps = selectionKey.interestOps();
        if ((interestOps & readInterestOp) == 0) {
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }
}
