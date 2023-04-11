基于版本：4.1.25.Final
Channel相关代码

继承关系：
NioServerSocketChannel << AbstractNioMessageChannel << AbstractNioChannel << AbstractChannel
NioSocketChannel << AbstractNioByteChannel << AbstractNioChannel << AbstractChannel << DefaultAttributeMap

class AbstractChannel{
	private final Unsafe unsafe;

	protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }

    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.bind(localAddress, promise);
    }
}

class NioServerSocketChannel extends AbstractNioMessageChannel{

  //AbstractChannel
  private final ChannelPipeline pipeline;
  
  // 继承自AbstractChannel
  private EventLoop eventLoop;
  
  public NioServerSocketChannel() {
    this(newSocket(DEFAULT_SELECTOR_PROVIDER)); //this(ServerSocketChannel.open())
  }

  public NioServerSocketChannel(ServerSocketChannel channel) {
    this.ch = ch;
    this.readInterestOp = SelectionKey.OP_ACCEPT;
    ch.configureBlocking(false);
    config = new NioServerSocketChannelConfig(this, javaChannel().socket());  //config = new NioServerSocketChannelConfig(this,channel.socket())

    pipeline = new DefaultChannelPipeline(this);
  }

  public NioServerSocketChannel(ServerSocketChannel channel) {
    super(null, channel, SelectionKey.OP_ACCEPT);
    config = new NioServerSocketChannelConfig(this, javaChannel().socket());
  }

  protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
    super(parent, ch, readInterestOp);
  }

  // AbstractNioChannel
  //用于unsafe调用
  protected void doRegister() throws Exception {
	    boolean selected = false;
	    for (;;) {
	        try {
	            selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);	//调用nio channel的register(selector,SelectionKey)方法，注册read事件
	            return;
	        } catch (CancelledKeyException e) {
	            if (!selected) {
	                // Force the Selector to select now as the "canceled" SelectionKey may still be
	                // cached and not removed because no Select.select(..) operation was called yet.
	                eventLoop().selectNow();
	                selected = true;
	            } else {
	                // We forced a select operation on the selector before but the SelectionKey is still cached
	                // for whatever reason. JDK bug ?
	                throw e;
	            }
	        }
	    }
	}

	//用于unsafe调用
	protected void doBind(SocketAddress localAddress) throws Exception {
        javaChannel().bind(localAddress, config.getBacklog());
  }

  // AbstractNioMessageChannel
  protected AbstractNioUnsafe newUnsafe() {
      return new NioMessageUnsafe();
  }
}


