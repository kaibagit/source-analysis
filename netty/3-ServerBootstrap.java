
基于版本：4.1.25.Final

class ServerBootstrap << AbstractBootstrap{
    volatile EventLoopGroup childGroup;  //worker
    volatile ChannelHandler childHandler;   //ChannelInitializer<SocketChannel>
    volatile EventLoopGroup group;   //boss
    //AbstractBootstrap
    private volatile ChannelHandler handler;

  
  
    //public ChannelFuture bind(int inetPort)最终会调用到该方法，启动入口
    //AbstractBootstrap方法
    private ChannelFuture doBind(final SocketAddress localAddress) {
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();

        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            // bind端口，并注册NIO accept事件
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        }
    }

    //AbstractBootstrap方法
    final ChannelFuture initAndRegister() {
        // 获取ServerSocketChannel对象，生成pipeline
        final Channel channel = channelFactory().newChannel();  //new NioServerSocketChannel();根据ServerBootstrap#channel(channelClass)来创建
        
        //配置options和attributes，在pipeline中添加自己的ChannelInitializer
        init(channel);

        //从EventLoopGroup中获取EventLoop，调用EventLoop.register()，最终会到达chanenl.unsafe().register(eventLoop)
        //chanenl.unsafe().register(eventLoop)做了如下事情：
        //将EventLoopGroup中获取EventLoop绑定到NioServerSocketChannel的eventLoop属性上，即boss线程与NioServerSocketChannel绑定
        //在nio的channel上注册0事件
        //会触发pipeline.fireChannelRegistered();
        ChannelFuture regFuture = group().register(channel);

        return regFuture;
    }

  
    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options();
        synchronized (options) {
            channel.config().setOptions(options);
        }

        final Map<AttributeKey<?>, Object> attrs = attrs();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;         //即worker
        final ChannelHandler currentChildHandler = childHandler;    //即ChannelInitializer<SocketChannel>
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(childOptions.size()));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
        }

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = handler();   //AbstractBootstrap.handler=null //即ServerBootstrap#handler(xxx)配置的handler
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
  
    // AbstractBootstrap方法
    private static void doBind0(
            final ChannelFuture regFuture, final Channel channel,
            final SocketAddress localAddress, final ChannelPromise promise) {

        // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
        // the pipeline in its channelRegistered() implementation.
        channel.eventLoop().execute(new OneTimeTask() {
            @Override
            public void run() {
                if (regFuture.isSuccess()) {
                    channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

}
















