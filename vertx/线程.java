基于版本 3.5.1

class VertxEventLoopGroup{

  private int pos;
  private final List<EventLoopHolder> workers = new ArrayList<>();

  public synchronized EventLoop next() {
    if (workers.isEmpty()) {
      throw new IllegalStateException();
    } else {
      EventLoop worker = workers.get(pos).worker;
      pos++;
      checkPos();
      return worker;
    }
  }

  public synchronized void addWorker(EventLoop worker) {
    EventLoopHolder holder = findHolder(worker);
    if (holder == null) {
      workers.add(new EventLoopHolder(worker));
    } else {
      holder.count++;
    }
  }
}

class HandlerManager{
  public synchronized void addHandler(T handler, ContextImpl context) {
    EventLoop worker = context.nettyEventLoop();
    availableWorkers.addWorker(worker);
    Handlers<T> handlers = new Handlers<>();
    Handlers<T> prev = handlerMap.putIfAbsent(worker, handlers);
    if (prev != null) {
      handlers = prev;
    }
    handlers.addHandler(new HandlerHolder<>(context, handler));
    hasHandlers = true;
  }
}




HttpServer线程总结：
class VertxImpl{
	EventLoopGroup acceptorEventLoopGroup;	//用于netty的boss线程，new时创建
	EventLoopGroup eventLoopGroup;			//用于netty的work线程，new时创建
}
class EventLoopContext{
	EventLoop eventLoop;	//在new时，会通过VertxImpl.eventLoopGroup调用next()获得
}
class HttpServerImpl{
	VertxEventLoopGroup availableWorkers = new VertxEventLoopGroup();
	HandlerManager<HttpHandlers> httpHandlerMgr = new HandlerManager<>(availableWorkers);
	// 在addHandler(T handler, ContextImpl context)时，会将context.eventLoop加入availableWorkers

	public HttpServer listen(xxx) {
		。。。
		ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(vertx.getAcceptorEventLoopGroup(), availableWorkers);
        。。。
        addHandlers(this, listenContext);
        。。。
	}
}