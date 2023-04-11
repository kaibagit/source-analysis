基于版本 3.5.1


// HttpServer用法
HttpServer server = vertx.createHttpServer();
server
  .requestHandler(router)
  .listen(portNumber, ar -> {
    if (ar.succeeded()) {
      startFuture.complete();
    } else {
      startFuture.fail(ar.cause());
    }
  });





class VertxImpl {

  private final Map<ServerID, HttpServerImpl> sharedHttpServers = new HashMap<>();

  public HttpServer createHttpServer(HttpServerOptions serverOptions{default:new HttpServerOptions}) {
    return new HttpServerImpl(this, serverOptions);
  }


}

class HttpServerImpl{

  private final HttpStreamHandler<HttpServerRequest> requestStream = new HttpStreamHandler<>();
  private final VertxEventLoopGroup availableWorkers = new VertxEventLoopGroup();
  private final HandlerManager<HttpHandlers> httpHandlerMgr = new HandlerManager<>(availableWorkers);

  public HttpServerImpl(VertxInternal vertx, HttpServerOptions options) {
    this.options = new HttpServerOptions(options);
    this.vertx = vertx;
    this.creatingContext = vertx.getContext();
    if (creatingContext != null) {
      if (creatingContext.isMultiThreadedWorkerContext()) {
        throw new IllegalStateException("Cannot use HttpServer in a multi-threaded worker verticle");
      }
      creatingContext.addCloseHook(this);
    }
    。。。
  }

  public synchronized HttpServer requestHandler(Handler<HttpServerRequest> handler) {
    requestStream.handler(handler);
    return this;
  }

  public synchronized HttpServer listen(int port, 
  									String host{default:"0.0.0.0"}, 
  									Handler<AsyncResult<HttpServer>> listenHandler{default:null}) {
    ..
    synchronized (vertx.sharedHttpServers()) {

      this.actualPort = port; // Will be updated on bind for a wildcard port
      id = new ServerID(port, host);
      HttpServerImpl shared = vertx.sharedHttpServers().get(id);
      //未创建过HttpServerImpl
      if (shared == null || port == 0) {
        serverChannelGroup = new DefaultChannelGroup("vertx-acceptor-channels", GlobalEventExecutor.INSTANCE);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(vertx.getAcceptorEventLoopGroup(), availableWorkers);
        applyConnectionOptions(bootstrap);
        ..
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
          @Override
            protected void initChannel(Channel ch) throws Exception {
              ..
            }
        });

        addHandlers(this, listenContext);
        ..
        bindFuture = AsyncResolveConnectHelper.doBind(vertx, SocketAddress.inetSocketAddress(port, host), bootstrap);
        bindFuture.addListener(res -> {
          if (res.failed()) {
            vertx.sharedHttpServers().remove(id);
          } else {
            Channel serverChannel = res.result();
            HttpServerImpl.this.actualPort = ((InetSocketAddress)serverChannel.localAddress()).getPort();
            serverChannelGroup.add(serverChannel);
            VertxMetrics metrics = vertx.metricsSPI();
            this.metrics = metrics != null ? metrics.createMetrics(this, new SocketAddressImpl(port, host), options) : null;
          }
        });
        ..
        vertx.sharedHttpServers().put(id, this);
        actualServer = this;
      // if (shared == null || port == 0) { 结束
      } else {	
        //HttpServerImpl已创建
        // Server already exists with that host/port - we will use that
        actualServer = shared;
        this.actualPort = shared.actualPort;
        addHandlers(actualServer, listenContext);
        VertxMetrics metrics = vertx.metricsSPI();
        this.metrics = metrics != null ? metrics.createMetrics(this, new SocketAddressImpl(port, host), options) : null;
      }


      actualServer.bindFuture.addListener(future -> {
        if (listenHandler != null) {
          final AsyncResult<HttpServer> res;
          if (future.succeeded()) {
            res = Future.succeededFuture(HttpServerImpl.this);
          } else {
            res = Future.failedFuture(future.cause());
            listening = false;
          }
          listenContext.runOnContext((v) -> listenHandler.handle(res));
        } else if (future.failed()) {
          listening  = false;
          // No handler - log so user can see failure
          log.error(future.cause());
        }
      });
    }
    return this;
  }

  private void addHandlers(HttpServerImpl server, ContextImpl context) {
    server.httpHandlerMgr.addHandler(
      new HttpHandlers(
        requestStream.handler(),
        wsStream.handler(),
        connectionHandler,
        exceptionHandler == null ? DEFAULT_EXCEPTION_HANDLER : exceptionHandler)
      , context);
  }



}