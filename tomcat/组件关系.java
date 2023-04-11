addContext()
getHost()
getEngine()
getServer()
getServer().findServices()[0];
service.getContainer()


// 层次结构：
tomcat{
	server(StandardServer){
		service(StandardService){
			Connector{
				ProtocolHandler(Http11NioProtocol){
					AbstractEndpoint(NioEndpoint){
                        executor(ThreadPoolExecutor)
                        Acceptor[]
                        Poller[]
                        Handler(ConnectionHandler)
                    }
				}
			}[]
			engine{
				host{
					Context{
						servlet
					}
				}
			}
		}[]
	}
}


Nio读写设计：
1、Acceptor线程通过 ServerSock.accept() 方式获取SocketChannel对象，将其封装成NioChannel，调用Poller.register(socket)进行注册
2、Poller将其封装成PollerEvent(NioChannel,NioSocketWrapper)，放入队列，唤醒Poller线程
3、Poller线程从events中取出PollerEvent并依次run，PollerEvent在run()时，会注册OP_READ事件
4、Poller线程调用selector.selectNow()方法，获取触发的读写事件
5、Poller会先注销IO关注事件，然后创建SocketProcessor，并提交给NioEndpoint的executor线程执行
6、如果SocketProcessor在处理时，如果数据能够读完，则会进入后面servlet处理逻辑
7、如果SocketProcessor发现数据没有读完，则终止执行，并重新注册read事件





Host,Context,Engine,Wrapper << Container{
	
}






StandardEngine << Engine{

}

StandardHost << Host{
	void startInternal(){

	}
}





Http11Processor{
	// AbstractProcessorLight
	SocketState process(SocketWrapperBase<?> socketWrapper, SocketEvent status){
		
	}
}










AbstractHttp11Processor{
	SocketState process(SocketWrapper<S> socketWrapper){
		getAdapter().service(request, response);
	}
}

CoyoteAdapter{
	void service(org.apache.coyote.Request req,
                        org.apache.coyote.Response res){
		connector.getService().getContainer().getPipeline().getFirst().invoke(request, response);
	}
}
















NioChannel{
    protected SocketChannel sc
    protected final SocketBufferHandler bufHandler;
}

NioSocketWrapper{
    private Poller poller = null;
    private int interestOps = 0;
}

NioSelectorPool{
	void open(){
		getSharedSelector();	//全局共享一个Selector
        if (SHARED) {
            blockingSelector = new NioBlockingSelector();
            blockingSelector.open(getSharedSelector());
        }
	}
}

NioBlockingSelector{
	void open(Selector selector){
		sharedSelector = selector;
        poller = new BlockPoller();
        // ...
        poller.start();
	}

	static class BlockPoller << Thread{
		SynchronizedQueue<Runnable> events = new SynchronizedQueue<>();

		void run(){
			while(run){
				events();	//从events队列中依次去除Runnable执行
				int keyCount = selector.selectNow();
				while (iterator.hasNext()) {
                    SelectionKey sk = iterator.next();
                    NioSocketWrapper attachment = (NioSocketWrapper)sk.attachment();
                    try {
                        iterator.remove();
                        sk.interestOps(sk.interestOps() & (~sk.readyOps()));
                        if ( sk.isReadable() ) {
                            countDown(attachment.getReadLatch());
                        }
                        if (sk.isWritable()) {
                            countDown(attachment.getWriteLatch());
                        }
                    }catch (CancelledKeyException ckx) {
                        sk.cancel();
                        countDown(attachment.getReadLatch());
                        countDown(attachment.getWriteLatch());
                    }
                }//while
			}
			events.clear();
		}
	}
}








NioReceiver{
    public void start(){
        bind();
        Thread t = new Thread(this,...);
        t.setDaemon(true);
        t.start();
    }

    void bind(){
        serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();
        this.selector.set(Selector.open());
        bind(serverSocket,getPort(),getAutoBind());
        serverChannel.configureBlocking(false);
        serverChannel.register(this.selector.get(), SelectionKey.OP_ACCEPT);
    }

    void run(){
        listen();
    }

    void listen(){
        events();
        socketTimeouts();
        int n = selector.select(getSelectorTimeout());
        if (n == 0) {
            continue;
        }

        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        // look at each key in the selected set
        while (it!=null && it.hasNext()) {
            SelectionKey key = it.next();
            // Is a new connection coming in?
            if (key.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel channel = server.accept();
                channel.socket().setReceiveBufferSize(getTxBufSize());
                channel.socket().setSendBufferSize(getTxBufSize());
                channel.socket().setTcpNoDelay(getTcpNoDelay());
                channel.socket().setKeepAlive(getSoKeepAlive());
                channel.socket().setOOBInline(getOoBInline());
                channel.socket().setReuseAddress(getSoReuseAddress());
                channel.socket().setSoLinger(getSoLingerOn(),getSoLingerTime());
                channel.socket().setSoTimeout(getTimeout());
                Object attach = new ObjectReader(channel);
                registerChannel(selector,
                                channel,
                                SelectionKey.OP_READ,
                                attach);
            }
            // is there data to read on this channel?
            if (key.isReadable()) {
                readDataFromSocket(key);
            } else {
                key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
            }

            // remove key from selected set, it's been handled
            it.remove();
        }
    }
}













Nio2Endpoint{
	void startInternal(){
	    if ( getExecutor() == null ) {
            createExecutor();
        }

        initializeConnectionLatch();
        startAcceptorThreads();
	}
}

