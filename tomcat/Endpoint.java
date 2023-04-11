class NioEndpoint extends AbstractEndpoint {
	private NioSelectorPool selectorPool = new NioSelectorPool();

	private ServerSocketChannel serverSock;

	private int pollerThreadCount = Math.min(2,Runtime.getRuntime().availableProcessors());

	private SynchronizedStack<PollerEvent> eventCache;	//PollerEvent对象池

	private SynchronizedStack<NioChannel> nioChannels;	//NioChannel对象池，减少gc

	// AbstractEndpoint
	void init(){
        ..
		bind();
        ..
	}
	void bind(){
		..
		serverSock.socket().bind(addr,getBacklog());
		serverSock.configureBlocking(true);
		..
		selectorPool.open();
	}

	void startInternal() {
        ..
        createExecutor();
        ..

        initializeConnectionLatch();

        // Start poller threads
        pollers = new Poller[getPollerThreadCount()];	//默认为CPU核数跟2取较小值
        for (int i=0; i<pollers.length; i++) {
            pollers[i] = new Poller();
            // ...
            pollerThread.start();
        }

        startAcceptorThreads();		//启动配置数量的Acceptor线程，默认为1
    }

	void initServerSocket(){
		serverSock = ServerSocketChannel.open();
		serverSock.socket().bind(addr,getAcceptCount());
		serverSock.configureBlocking(true);
	}

    SocketChannel serverSocketAccept() throws Exception {
        return serverSock.accept();
    }

    // 由Accept线程调用
    boolean setSocketOptions(SocketChannel socket) {
        // Process the connection
        try {
            //disable blocking, APR style, we are gonna be polling it
            socket.configureBlocking(false);
            Socket sock = socket.socket();
            socketProperties.setProperties(sock);

            NioChannel channel = nioChannels.pop();
            if (channel == null) {
                SocketBufferHandler bufhandler = new SocketBufferHandler(
                        socketProperties.getAppReadBufSize(),
                        socketProperties.getAppWriteBufSize(),
                        socketProperties.getDirectBuffer());

                channel = new NioChannel(socket, bufhandler);
            } else {
                channel.setIOChannel(socket);
                channel.reset();
            }
            getPoller0().register(channel);		//getPoller0()通过负载均衡获取Poller
        } catch (Throwable t) {
        	// ...
            return false;
        }
        return true;
    }

    // AbstractEndpoint
    boolean processSocket(SocketWrapperBase<S> socketWrapper,
        ..
    }

    SocketProcessorBase<NioChannel> createSocketProcessor(
            SocketWrapperBase<NioChannel> socketWrapper, SocketEvent event) {
        return new SocketProcessor(socketWrapper, event);
    }

    SocketProcessor{
    	protected SocketWrapperBase<S> socketWrapper;
    	protected SocketEvent event;

    	void doRun(){
    		SocketState state = SocketState.OPEN;
    		state = getHandler().process(socketWrapper, SocketEvent.OPEN_READ);
    	}
    }
    
}


















class ConnectionHandler<S> implements AbstractEndpoint.Handler<S>{

    public SocketState process(SocketWrapperBase<S> wrapper, SocketEvent status) {
        ..
        Processor processor = connections.get(socket);
        ..
        if (processor == null) {
            processor = getProtocol().createProcessor();
            register(processor);
        }
        ..
        connections.put(socket, processor);
        ..
        state = processor.process(wrapper, status);
        ..
        if (state == SocketState.LONG) {
            // In the middle of processing a request/response. Keep the
            // socket associated with the processor. Exact requirements
            // depend on type of long poll
            longPoll(wrapper, processor);       //注册read事件，socket.registerReadInterest();
            ..
        } else if (state == SocketState.OPEN) {
            // In keep-alive but between requests. OK to recycle
            // processor. Continue to poll for the next request.
            connections.remove(socket);
            release(processor);
            wrapper.registerReadInterest();
        }
        ..
        connections.remove(socket);
        ..
        release(processor);
    }
}






















class Http11Processor extends AbstractProcessor{

    // AbstractProcessorLight
    public SocketState process(SocketWrapperBase<?> socketWrapper, SocketEvent status){
        ..
        if (status == SocketEvent.OPEN_READ){
            state = service(socketWrapper);
        }
    }

    // 当request读取完成，返回SocketState.OPEN；否则返回SocketState.LONG
    public SocketState service(SocketWrapperBase<?> socketWrapper){
        ..
        inputBuffer.init(socketWrapper);
        ..
        inputBuffer.parseRequestLine(keptAlive)
        ..
        if (!inputBuffer.parseHeaders()) {
            // We've read part of the request, don't recycle it
            // instead associate it with the socket
            openSocket = true;
            readComplete = false;
            ..
        }
        ..
        getAdapter().service(request, response);    //最终会调用servlet方法
        ..
        if (readComplete) {
            return SocketState.OPEN;
        } else {
            return SocketState.LONG;
        }
        ..
    }

}






















class Http11InputBuffer{

    void init(SocketWrapperBase<?> socketWrapper) {
        ..
    }

    boolean parseRequestLine(boolean keptAlive) {
        ..
    }
}