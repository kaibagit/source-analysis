interface ChannelHandlerContext{

    //
    // 入站事件
    // fireXXX，都是流转到到下一个handler处理
    //
    ChannelHandlerContext fireChannelRegistered();

    ChannelHandlerContext fireChannelUnregistered();

    ChannelHandlerContext fireChannelActive();

    ChannelHandlerContext fireChannelInactive();

    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    ChannelHandlerContext fireUserEventTriggered(Object evt);

    ChannelHandlerContext fireChannelRead(Object msg);

    ChannelHandlerContext fireChannelReadComplete();

    ChannelHandlerContext fireChannelWritabilityChanged();

    ChannelHandlerContext read();


    //
    // 出站事件
    //
    ChannelFuture write(Object msg);
}



// 继承关系：
DefaultChannelHandlerContext << AbstractChannelHandlerContext << DefaultAttributeMap



class AbstractChannelHandlerContext{
    final EventExecutor executor;

    // 获取当前Context的EventExecutor
    // 如果没有在ChannelPipeline#addLast(EventExecutorGroup group, String name, ChannelHandler handler)时指定，则为null
    // 为null时，最后取当前channel所绑定的EventLoop
    public EventExecutor executor() {
        if (executor == null) {
            return channel().eventLoop();
        } else {
            return executor;
        }
    }
}