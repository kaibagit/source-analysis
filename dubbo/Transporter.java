基于版本：2.5.7
Transporter相关源码

@SPI("netty")
interface Transporter {

    @Adaptive({Constants.SERVER_KEY, Constants.TRANSPORTER_KEY})
    Server bind(URL url, ChannelHandler handler) throws RemotingException;

    // 与server建立连接
    @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    Client connect(URL url, ChannelHandler handler) throws RemotingException;

}


// netty 4
public class NettyTransporter implements Transporter {

    public static final String NAME = "netty4";
    
    public Server bind(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyServer(url, listener);
    }

    // 创建Bootstrap，与server建立连接
    public Client connect(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyClient(url, listener);
    }

}

class NettyServer{
    // AbstractPeer
    private final ChannelHandler handler;   // dubbo自己的ChannelHandler

    public NettyServer(URL url, ChannelHandler handler){
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    // AbstractPeer
    public void received(Channel ch, Object msg) throws RemotingException {
        。。。
        handler.received(ch, msg);
    }

    protected void doOpen() throws Throwable {
        NettyHelper.setNettyLoggerFactory();

        bootstrap = new ServerBootstrap();

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        workerGroup = new NioEventLoopGroup(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                new DefaultThreadFactory("NettyServerWorker", true));

        final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
        channels = nettyServerHandler.getChannels();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                                .addLast("decoder", adapter.getDecoder())
                                .addLast("encoder", adapter.getEncoder())
                                .addLast("handler", nettyServerHandler);
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();

    }
}

// 将netty本身的所有事件，转发给NettyServer处理
class NettyServerHandler extends ChannelDuplexHandler{

    private final URL url;

    private final ChannelHandler handler;   // dubbo自己的ChannelHandler，实现类为NettyServer

    public NettyServerHandler(URL url, ChannelHandler handler) {
        。。。
        this.url = url;
        this.handler = handler;
    }
}








// NettyClient创建时，就会与server建立连接
class NettyClient{
	public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);

        send_reconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, false);

        shutdown_timeout = url.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);

        //默认重连间隔2s，1800表示1小时warning一次.
        reconnect_warning_period = url.getParameter("reconnect.waring.period", 1800);

        try {
        	// 初始化，创建Bootstrap
            doOpen();
        } catch (Throwable t) {
        	。。。
        }
        try {
            // 与server建立连接
            connect();
            。。。
        } catch (Throwable t) {
            。。。
        }

        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(DataStore.class)
                .getDefaultExtension().get(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
        ExtensionLoader.getExtensionLoader(DataStore.class)
                .getDefaultExtension().remove(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
    }
}
