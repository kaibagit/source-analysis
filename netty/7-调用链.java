NioEventLoopGroup.register(channel) => NioEventLoop.register(channel) => channel.unsafe().register(channel, promise)

ServerBootstrap生命周期：
->AbstractBootstrap(ServerBootstrap)#bind(xxx)
	->AbstractBootstrap(ServerBootstrap)#doBind0(xxx)
		->AbstractBootstrap(ServerBootstrap)#initAndRegister()
			->ServerBootstrap#init(channel)

bind端口流程：
->AbstractBootstrap(ServerBootstrap)#bind(xxx)
	->AbstractBootstrap(ServerBootstrap)#doBind(localAddress)
		->AbstractBootstrap(ServerBootstrap)#doBind0(xxx)
			->NioServerSocketChannel#bind(localAddress, promise)
				->DefaultChannelPipeline#bind(localAddress, promise)
					->TailContext#bind(localAddress, promise)
						->HeadContext#invokeBind(localAddress, promise)
							->HeadContext#bind(ctx,localAddress,promise)
								->AbstractUnsafe(NioSocketChannelUnsafe)#bind(localAddress, promise)
									->NioSocketChannelUnsafe#doBind(localAddress)
										->ServerSocketChannel#socket()#bind(localAddress,backlog)

注册监听事件：
->AbstractUnsafe(NioSocketChannelUnsafe)#bind(localAddress, promise)
	->DefaultChannelPipeline#fireChannelActive()
		->AbstractChannelHandlerContext#invokeChannelActive(head)
			->AbstractChannelHandlerContext(HeadContext)#invokeChannelActive()
				->HeadContext#channelActive(ctx)
					->HeadContext#readIfIsAutoRead()
						->AbstractChannel(ServerSocketChannel)#read()
							->DefaultChannelPipeline#read()
								->AbstractChannelHandlerContext(TailContext)#read()
									->AbstractChannelHandlerContext(HeadContext)#invokeRead()
										->TailContext#read(ctx)
											->AbstractUnsafe(NioMessageUnsafe)#beginRead()
												->AbstractNioUnsafe(NioMessageUnsafe)#doBeginRead()

// Selector#select()相关api调用：
->AbstractBootstrap(ServerBootstrap)#bind(inetPort)
	->AbstractBootstrap(ServerBootstrap)#bind(localAddress)
		->AbstractBootstrap(ServerBootstrap)#doBind(localAddress)
			->AbstractBootstrap(ServerBootstrap)#initAndRegister()
				->MultithreadEventLoopGroup(NioEventLoopGroup)#register(channel)
					->SingleThreadEventLoop(NioEventLoop)#register(channel)
						->SingleThreadEventLoop(NioEventLoop)#register(promise)
							->AbstractUnsafe(NioMessageUnsafe)#register(eventLoop,promise)
								->SingleThreadEventExecutor(NioEventLoop)#execute(task)
									->AbstractUnsafe(NioMessageUnsafe)#register0(promise)
										->SingleThreadEventExecutor(NioEventLoop)#startThread()
											->SingleThreadEventExecutor(NioEventLoop)#doStartThread()
												||->NioEventLoop#run()
													->NioEventLoop#select(oldWakenUp)
														->Selector#selectNow()



Channel read调用链：
NioEventLoop#run()
->NioEventLoop#processSelectedKeys()
	->NioEventLoop#processSelectedKeysOptimized()
		->NioEventLoop#processSelectedKey(SelectionKey,AbstractNioChannel)
			->NioUnsafe(NioSocketChannelUnsafe)#read()		//在这里将数据写入ByteBuf
				->ChannelPipeline#fireChannelRead(Object)
					->HeadContext#fireChannelRead(Object)
						->AbstractChannelHandlerContext#fireChannelRead(msg)
							->ChannelInboundHandler#channelRead(ctx,msg)

ChannelPipeline#fireChannelRead(#)核心流程:
1、依次往后next找Inbound的ChannelHandlerContext，然后调用ChannelHandlerContext#fireChannelRead(msg)方法
2、然后找到TailContext，调用ChannelHandlerContext#fireChannelRead(msg)方法，最后调用到TailContext#channelRead(ctx,msg)
3、TailContext#channelRead(ctx,msg)做也不做，只是释放msg的内存



NioUnsafe(NioSocketChannelUnsafe)#read() 核心流程（分配内存）：
->NioUnsafe(NioSocketChannelUnsafe)#read()
	->RecvByteBufAllocator.Handle(AdaptiveRecvByteBufAllocator$HandlerImpl)#allocate(allocator)
		->ByteBufAllocator(PooledByteBufAllocator)#ioBuffer(#)
			->AbstractByteBufAllocator(PooledByteBufAllocator)#ioBuffer(initialCapacity)
				->AbstractByteBufAllocator(PooledByteBufAllocator)#directBuffer(initialCapacity)
					->AbstractByteBufAllocator(PooledByteBufAllocator)#directBuffer(initialCapacity,maxCapacity)
						->PooledByteBufAllocator#newDirectBuffer(initialCapacity,maxCapacity)
							->PoolArena#allocate(cache, initialCapacity, maxCapacity)





inbound异常的处理
1、ChannelHandlerContext捕获到异常之后，会触发当前handler的exceptionCaught(ChannelHandlerContext, Throwable)方法
2、如果当前handler不处理，默认会提交给next ChannelHandlerContext处理
3、最后会流转到TailContext



// ChannelHandlerContext write调用链：
ChannelHandlerContext(AbstractChannelHandlerContext)#write(msg)
->ChannelHandlerContext(AbstractChannelHandlerContext)#write(msg,promise)
	->ChannelHandlerContext(AbstractChannelHandlerContext)#write(msg,flush,promise)
		->ChannelHandlerContext(AbstractChannelHandlerContext)#invokeWrite(msg,promise)
			->ChannelHandlerContext(AbstractChannelHandlerContext)#invokeWrite0(msg,promise)
				->HeadContext#write(msg,promise)
					->Unsafe#write(msg,promise)
// ChannelHandlerContext#write(msg)核心流程:
// 1、依次往前pre找Outbound的ChannelHandlerContext，然后调用ChannelHandlerContext#invokeWrite(msg,promise)方法
// 2、然后找到HeadContext，调用ChannelHandlerContext#invokeWrite(msg,promise)方法，最后调用到HeadContext#write(msg,promise)
// 3、最后调用Unsafe#write(msg,promise)





//NioServerSocketChannel事件流：
AbstractNioChannel(NioServerSocketChannel)#doRegister() 调用NIO API 获取selectionKey，attachment为NioServerSocketChannel
	selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
AbstractNioUnsafe(NioMessageUnsafe)#doBeginRead() 注册监听事件；对于NioServerSocketChannel，readInterestOp = SelectionKey.OP_ACCEPT;
	selectionKey.interestOps(interestOps | readInterestOp);
NioEventLoop#run()最终会调用到 => NioMessageUnsafe#read() => NioServerSocketChannel#doReadMessages(readBuf) 最终调用ServerSocketChannel#accept()
	然后创建一个新的NioSocketChannel，触发ChannelPipeline#fireChannelRead(msg)和ChannelPipeline#fireChannelReadComplete()



// 组件间关系：
ChannelPipeline{
	AbstractChannelHandlerContext head;
	AbstractChannelHandlerContext tail;
	Channel channel;
}
ChannelHandlerContext{
	AbstractChannelHandlerContext next;
	AbstractChannelHandlerContext prev;
	DefaultChannelPipeline pipeline;
	EventExecutor executor;
	ChannelHandler handler;
}


封装关系
1、ChannelInitializer#initChannel(SocketChannel ch)，会调用pipeline.addLast("handler", new HelloServerHandler());
2、pipeline会将ChannelHandler封装到新创建ChannelHandlerContext实例中，并加入到ChannelHandlerContext的双向链表中










