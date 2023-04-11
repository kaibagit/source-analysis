// NioEndpoint内部类
class Poller << Runnable{

	private Selector selector;
	private final SynchronizedQueue<PollerEvent> events = new SynchronizedQueue<>();   //自动扩容的无界队列
	// events队列中的数量，
	private AtomicLong wakeupCounter = new AtomicLong(0);


	public Poller() throws IOException {
	    this.selector = Selector.open();
	}


	// 由Accept线程调用
	public void register(final NioChannel socket) {
	    socket.setPoller(this);
        NioSocketWrapper ka = new NioSocketWrapper(socket, NioEndpoint.this);
        socket.setSocketWrapper(ka);
        ka.setPoller(this);
        .. //配置ka属性
        ka.interestOps(SelectionKey.OP_READ);//this is what OP_REGISTER turns into.
        ..
        PollerEvent r = new PollerEvent(socket,ka,OP_REGISTER);
        ..
        addEvent(r);
	}
	private void addEvent(PollerEvent event) {
	    events.offer(event);
	    if ( wakeupCounter.incrementAndGet() == 0 ) selector.wakeup();	//有新的event，唤醒在selector.select()方法上阻塞的线程
	}


    // Poller线程主入口
	public void run() {
		while(true){
			..
			hasEvents = events();	//从events中取出PollerEvent并依次run
			..
		    if (wakeupCounter.getAndSet(-1) > 0) {    //wakeupCounter的值>0 说明有待处理event
                //if we are here, means we have other stuff to do
                //do a non blocking select
                keyCount = selector.selectNow();
            } else {
                keyCount = selector.select(selectorTimeout);
            }
            wakeupCounter.set(0);
            ..
            selector.selectedKeys().each |SelectionKey sk|{
            	NioSocketWrapper attachment = (NioSocketWrapper)sk.attachment();
		        // Attachment may be null if another thread has called
                // cancelledKey()
                if (attachment == null) {
                    iterator.remove();
                } else {
                    iterator.remove();
                    processKey(sk, attachment);
                }
            }
		    ..
		}
	}

	void processKey(SelectionKey sk, NioSocketWrapper attachment) {
		..
	    if (sk.isReadable() || sk.isWritable() ) {
            if ( attachment.getSendfileData() != null ) {
                processSendfile(sk,attachment, false);
            } else {
                unreg(sk, attachment, sk.readyOps());
                boolean closeSocket = false;
                // Read goes before write
                if (sk.isReadable()) {
                    if (!processSocket(attachment, SocketEvent.OPEN_READ, true)) {
                        closeSocket = true;
                    }
                }
                if (!closeSocket && sk.isWritable()) {
                    if (!processSocket(attachment, SocketEvent.OPEN_WRITE, true)) {
                        closeSocket = true;
                    }
                }
                if (closeSocket) {
                    cancelledKey(sk);
                }
            }
        }
        ..
	}

	public boolean events() {
	    boolean result = false;

	    PollerEvent pe = null;
	    while ( (pe = events.poll()) != null ) {
	        result = true;
	        try {
	            pe.run();
	            pe.reset();
	            if (running && !paused) {
	                eventCache.push(pe);
	            }
	        } catch ( Throwable x ) {
	            log.error("",x);
	        }
	    }

	    return result;
	}

}

















class NioEndpoint{

	// AbstractEndpoint
    boolean processSocket(SocketWrapperBase<S> socketWrapper,
        SocketEvent event, boolean dispatch) {
        ..
        SocketProcessorBase<S> sc = createSocketProcessor(socketWrapper, event);
        ..
        Executor executor = getExecutor();
        if (dispatch && executor != null) {
            executor.execute(sc);
        } else {
            sc.run();
        }
        ..
        return true;
    }

}






class SocketProcessorBase<S> implements Runnable{

	public final void run() {
        ..
        doRun();
        ..
    }
}
// NioEndpoint内部类
class SocketProcessor extends SocketProcessorBase<NioChannel> {

    // 继承自SocketProcessorBase
    protected SocketEvent event;

	protected void doRun() {
		..
		state = getHandler().process(socketWrapper, event);
		..
	}
}













// 主要用于注册io事件
class PollerEvent implements Runnable{

    private NioChannel socket;
    private int interestOps;       //关注的事件
    private NioSocketWrapper socketWrapper;

    void run(){
    	if (interestOps == OP_REGISTER) {
            ..
    		socket.getIOChannel().register(
                            socket.getPoller().getSelector(), SelectionKey.OP_READ, socketWrapper);
            ..
    	}else{
            ..
    		final SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
            ..
    		final NioSocketWrapper socketWrapper = (NioSocketWrapper) key.attachment();
    		int ops = key.interestOps() | interestOps;
            socketWrapper.interestOps(ops);
            key.interestOps(ops);
            ..
    	}
    }
}