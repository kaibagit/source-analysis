事件:
in:
    //A Channel was registered to its EventLoop.
    ChannelPipeline#fireChannelRegistered() =>  ChannelInboundHandler#channelRegistered(ChannelHandlerContext)
    //A Channel was unregistered from its EventLoop.
    ChannelPipeline#fireChannelUnregistered() => ChannelInboundHandler#channelUnregistered(ChannelHandlerContext)
    //A Channel is active now, which means it is connected. 
    ChannelPipeline#fireChannelActive() => ChannelInboundHandler#channelActive(ChannelHandlerContext)
    //A Channel is inactive now, which means it is closed. 
    ChannelPipeline#fireChannelInactive() => ChannelInboundHandler#channelInactive(ChannelHandlerContext)
    //A Channel received an Throwable in one of its inbound operations.
    ChannelPipeline#fireExceptionCaught(Throwable cause) => ChannelHandlerContext#invokeExceptionCaught(Throwable cause) =>ChannelInboundHandler#exceptionCaught(ChannelHandlerContext, Throwable)
    //A Channel received an user defined event. 
    ChannelPipeline#fireUserEventTriggered(Object event) => ChannelInboundHandler#userEventTriggered(ChannelHandlerContext, Object)
    //A Channel received a message. 
    ChannelPipeline#fireChannelRead(Object msg) => ChannelHandlerContext#invokeChannelRead(msg) => ChannelInboundHandler#channelRead(ChannelHandlerContext, msg)
    ChannelPipeline#fireChannelReadComplete() => ChannelInboundHandler#channelReadComplete(ChannelHandlerContext)
    ChannelPipeline#fireChannelWritabilityChanged() => ChannelInboundHandler#channelWritabilityChanged(ChannelHandlerContext)
out:
    ChannelHandlerContext#write(msg) => ChannelOutboundHandler#write(ctx,msg,promise)