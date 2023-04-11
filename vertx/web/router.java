基于版本 3.5.1

// 通过Router将所有的Route排序好，存放到routes属性中
// 在Router#accept(request)时，创建一个RoutingContext，生成routes的iterator
// 调用RoutingContext的next()方法，会依次遍历routes的iterator，对该RoutingContext进行处理
// 如果在这过程中失败了，比如抛出异常，或者是调用了RoutingContext#fail()，则会对RoutingContext的failure进行设置，并重置routes的iterator；
// 		依次遍历routes的iterator，对该RoutingContext进行handleFailure处理

class RouterImpl{

  // 排好序的route列表
  final Set<RouteImpl> routes = new ConcurrentSkipListSet<>(routeComparator);

  void accept(HttpServerRequest request) {
		。。。
	    (new RoutingContextImpl((String)null, this, request, this.routes)).next();
  }

  void add(RouteImpl route) {
    routes.add(route);
  }

  void remove(RouteImpl route) {
    routes.remove(route);
  }
}

// 每个请求对应一个RoutingContext
class RoutingContextImpl{

  // RoutingContextImplBase
  Iterator<RouteImpl> iter;

  // RoutingContextImplBase
  RouteImpl currentRoute;	//当前正在执行的Route，用于执行完阻塞型handler时，能够找到处理到哪了

  // RoutingContextImplBase
  AtomicInteger currentRouteNextHandlerIndex;		//用于记录currentRoute中，在处理的handler索引，用于一个Route有多个handler的情况
  AtomicInteger currentRouteNextFailureHandlerIndex;

  public RoutingContextImpl(String mountPoint, RouterImpl router, HttpServerRequest request, Set<RouteImpl> routes) {
    super(mountPoint, request, routes);
    this.router = router;

    fillParsedHeaders(request);
    if (request.path().charAt(0) != '/') {
      fail(404);
    }
  }

  protected RoutingContextImplBase(String mountPoint, HttpServerRequest request, Set<RouteImpl> routes) {
    this.mountPoint = mountPoint;
    this.request = new HttpServerRequestWrapper(request);
    this.routes = routes;
    this.iter = routes.iterator();
    currentRouteNextHandlerIndex = new AtomicInteger(0);
    currentRouteNextFailureHandlerIndex = new AtomicInteger(0);
  }

  public void next() {
        if(!this.iterateNext()) {
            this.checkHandleNoMatch();
        }
    }


  public void fail(Throwable t) {
    this.failure = t == null ? new NullPointerException() : t;	//标记为失败
    doFail();
  }
  private void doFail() {
    this.iter = router.iterator();	//重置routes的iterator，相当于重新处理request，知不是这次是默认fail
    currentRoute = null;
    next();
  }

    protected boolean iterateNext() {
	    boolean failed = failed();

	    // 用来处理一个Route有多个handler的情况
	    if (currentRoute != null) { // Handle multiple handlers inside route object
	      try {
	        if (!failed && currentRoute.hasNextContextHandler(this)) {
	          currentRouteNextHandlerIndex.incrementAndGet();
	          currentRoute.handleContext(this);
	          return true;
	        } else if (failed && currentRoute.hasNextFailureHandler(this)) {
	          currentRouteNextFailureHandlerIndex.incrementAndGet();
	          currentRoute.handleFailure(this);
	          return true;
	        }
	      } catch (Throwable t) {
	        if (log.isTraceEnabled()) log.trace("Throwable thrown from handler", t);
	        if (!failed) {
	          if (log.isTraceEnabled()) log.trace("Failing the routing");
	          fail(t);
	        } else {
	          // Failure in handling failure!
	          if (log.isTraceEnabled()) log.trace("Failure in handling failure");
	          unhandledFailure(-1, t, currentRoute.router());
	        }
	        return true;
	      }
	    }

	    // 遍历所有的Route，对RoutingContext进行处理
	    while (iter.hasNext()) { // Search for more handlers
	      RouteImpl route = iter.next();
	      currentRouteNextHandlerIndex.set(0);
	      currentRouteNextFailureHandlerIndex.set(0);
	      try {
	        if (route.matches(this, mountPoint(), failed)) {
	          。。。
	          try {
	          	// 记录当前Route，因为在route#handleContext(this)时，有可能会切换到阻塞型的线程池处理
	            currentRoute = route;
	            。。。
	            if (failed && currentRoute.hasNextFailureHandler(this)) {
	              currentRouteNextFailureHandlerIndex.incrementAndGet();
	              route.handleFailure(this);
	            } else if (currentRoute.hasNextContextHandler(this)) {
	              currentRouteNextHandlerIndex.incrementAndGet();
	              route.handleContext(this);
	            } else {
	              continue;
	            }
	          } catch (Throwable t) {
	            。。。
	            if (!failed) {
	              。。。
	              fail(t);
	            } else {
	              。。。
	              unhandledFailure(-1, t, route.router());
	            }
	          }
	          return true;
	        }
	      } catch (IllegalArgumentException e) {
	        。。。
	        // Failure in handling failure!
	        unhandledFailure(400, e, route.router());
	        return true;
	      }
	    }
	    return false;
	  }
}


class RouteImpl{

  private List<Handler<RoutingContext>> contextHandlers;	//处理RoutingContext的handler列表
  private List<Handler<RoutingContext>> failureHandlers;

  // 关联的Router实例，该Route在Router的顺序
  RouteImpl(RouterImpl router, int order) {
    this.router = router;
    this.order = order;
    this.contextHandlers = new ArrayList<>();
    this.failureHandlers = new ArrayList<>();
  }


  // 增加handler
  public synchronized Route handler(Handler<RoutingContext> contextHandler) {
    this.contextHandlers.add(contextHandler);
    checkAdd();
    return this;
  }

  // 增加阻塞型handler
  public synchronized Route blockingHandler(Handler<RoutingContext> contextHandler, boolean ordered) {
    return handler(new BlockingHandlerDecorator(contextHandler, ordered));
  }

  // 增加失败处理handler
  public synchronized Route failureHandler(Handler<RoutingContext> exceptionHandler) {
    this.failureHandlers.add(exceptionHandler);
    checkAdd();
    return this;
  }


  synchronized void handleContext(RoutingContextImplBase context) {
    contextHandlers.get(context.currentRouteNextHandlerIndex() - 1).handle(context);
  }

  synchronized void handleFailure(RoutingContextImplBase context) {
    failureHandlers.get(context.currentRouteNextFailureHandlerIndex() - 1).handle(context);
  }



  synchronized boolean matches(RoutingContextImplBase context, String mountPoint, boolean failure) {

    if (failure && !hasNextFailureHandler(context) || !failure && !hasNextContextHandler(context)) {
      return false;
    }
    if (!enabled) {
      return false;
    }
    HttpServerRequest request = context.request();
    if (!methods.isEmpty() && !methods.contains(request.method())) {
      return false;
    }
    if (path != null && pattern == null && !pathMatches(mountPoint, context)) {
      return false;
    }
    if (pattern != null) {
      String path = useNormalisedPath ? Utils.normalizePath(context.request().path()) : context.request().path();
      if (mountPoint != null) {
        path = path.substring(mountPoint.length());
      }

      Matcher m = pattern.matcher(path);
      if (m.matches()) {
        if (m.groupCount() > 0) {
          if (groups != null) {
            // Pattern - named params
            // decode the path as it could contain escaped chars.
            for (int i = 0; i < groups.size(); i++) {
              final String k = groups.get(i);
              String undecodedValue;
              // We try to take value in three ways:
              // 1. group name of type p0, p1, pN (most frequent and used by vertx params)
              // 2. group name inside the regex
              // 3. No group name
              try {
                undecodedValue = m.group("p" + i);
              } catch (IllegalArgumentException e) {
                try {
                  undecodedValue = m.group(k);
                } catch (IllegalArgumentException e1) {
                  // Groups starts from 1 (0 group is total match)
                  undecodedValue = m.group(i + 1);
                }
              }
              addPathParam(context, k, undecodedValue);
            }
          } else {
            // Straight regex - un-named params
            // decode the path as it could contain escaped chars.
            for (String namedGroup : namedGroupsInRegex) {
              String namedGroupValue = m.group(namedGroup);
              if (namedGroupValue != null) {
                addPathParam(context, namedGroup, namedGroupValue);
              }
            }
            for (int i = 0; i < m.groupCount(); i++) {
              String group = m.group(i + 1);
              if (group != null) {
                final String k = "param" + i;
                addPathParam(context, k, group);
              }
            }
          }
        }
      } else {
        return false;
      }
    }

    // Check if query params are already parsed
    if (context.queryParams().size() == 0) {
      // Decode query parameters and put inside context.queryParams
      Map<String, List<String>> decodedParams = new QueryStringDecoder(request.uri()).parameters();

      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet())
        context.queryParams().add(entry.getKey(), entry.getValue());
    }

    if (!consumes.isEmpty()) {
      // Can this route consume the specified content type
      MIMEHeader contentType = context.parsedHeaders().contentType();
      MIMEHeader consumal = contentType.findMatchedBy(consumes);
      if (consumal == null) {
        return false;
      }
    }
    List<MIMEHeader> acceptableTypes = context.parsedHeaders().accept();
    if (!produces.isEmpty() && !acceptableTypes.isEmpty()) {
      MIMEHeader selectedAccept = context.parsedHeaders().findBestUserAcceptedIn(acceptableTypes, produces);
      if (selectedAccept != null) {
        context.setAcceptableContentType(selectedAccept.rawValue());
        return true;
      }
      return false;
    }
    return true;
  }
}

// 将阻塞型Handler封装成非阻塞
class BlockingHandlerDecorator{
  
  private boolean ordered;
  private final Handler<RoutingContext> decoratedHandler;
  
  public BlockingHandlerDecorator(Handler<RoutingContext> decoratedHandler, boolean ordered) {
    Objects.requireNonNull(decoratedHandler);
    this.decoratedHandler = decoratedHandler;
    this.ordered = ordered;
  }
  
  @Override
  public void handle(RoutingContext context) {
    Route currentRoute = context.currentRoute();
    context.vertx().executeBlocking(fut -> {
      decoratedHandler.handle(new RoutingContextDecorator(currentRoute, context));
      fut.complete();
    }, ordered, res -> {
      if (res.failed()) {
        // This means an exception was thrown from the blocking handler
        context.fail(res.cause());
      }
    });
  }
}