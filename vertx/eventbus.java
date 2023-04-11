基于版本 3.5.1

class VertxImpl{
	VertxImpl(VertxOptions options, Handler<AsyncResult<Vertx>> resultHandler) {
		。。。
		createAndStartEventBus(options, resultHandler);
		。。。
	}
	void createAndStartEventBus(VertxOptions options, Handler<AsyncResult<Vertx>> resultHandler) {
		if (options.isClustered()) {
	      eventBus = new ClusteredEventBus(this, options, clusterManager, haManager);
	    } else {
	      eventBus = new EventBusImpl(this);
	    }
	    eventBus.start(ar -> {
	      。。。
	    });
	}
}

class EventBusImpl{
  List<Handler<SendContext>> interceptors = new CopyOnWriteArrayList<>();	//EventBus的Message拦截器列表

  public synchronized void start(Handler<AsyncResult<Void>> completionHandler) {
	。。。
    started = true;
    completionHandler.handle(Future.succeededFuture());
  }



  // 消费message
  public <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler) {
    。。。
    MessageConsumer<T> consumer = consumer(address);
    consumer.handler(handler);
    return consumer;
  }

  public <T> MessageConsumer<T> consumer(String address) {
    。。。
    return new HandlerRegistration<>(vertx, metrics, this, address, null, false, null, -1);
  }




  public EventBus publish(String address, Object message) {
    return publish(address, message, new DeliveryOptions());
  }

  public EventBus publish(String address, Object message, DeliveryOptions options) {
    sendOrPubInternal(
    	createMessage(false, address, options.getHeaders(), message, options.getCodecName()), 
    	options, null);
    return this;
  }

  protected MessageImpl createMessage(boolean send, String address, MultiMap headers, Object body, String codecName) {
    。。。
    MessageCodec codec = codecManager.lookupCodec(body, codecName);
    MessageImpl msg = new MessageImpl(address, null, headers, body, codec, send, this);
    return msg;
  }

  private <T> void sendOrPubInternal(MessageImpl message, DeliveryOptions options,
                                     Handler<AsyncResult<Message<T>>> replyHandler) {
    。。。
    HandlerRegistration<T> replyHandlerRegistration = createReplyHandlerRegistration(message, options, replyHandler);
    SendContextImpl<T> sendContext = new SendContextImpl<>(message, options, replyHandlerRegistration);
    sendContext.next();
  }

  private <T> HandlerRegistration<T> createReplyHandlerRegistration(MessageImpl message,
                                                                    DeliveryOptions options,
                                                                    Handler<AsyncResult<Message<T>>> replyHandler) {
    if (replyHandler != null) {
      long timeout = options.getSendTimeout();
      String replyAddress = generateReplyAddress();
      message.setReplyAddress(replyAddress);
      Handler<Message<T>> simpleReplyHandler = convertHandler(replyHandler);
      HandlerRegistration<T> registration =
        new HandlerRegistration<>(vertx, metrics, this, replyAddress, message.address, true, replyHandler, timeout);
      registration.handler(simpleReplyHandler);
      return registration;
    } else {
      return null;
    }
  }

  protected <T> void sendOrPub(SendContextImpl<T> sendContext) {
    MessageImpl message = sendContext.message;
    。。。
    deliverMessageLocally(sendContext);
  }

  protected <T> void deliverMessageLocally(SendContextImpl<T> sendContext) {
    if (!deliverMessageLocally(sendContext.message)) {
      。。。
      if (sendContext.handlerRegistration != null) {
        sendContext.handlerRegistration.sendAsyncResultFailure(ReplyFailure.NO_HANDLERS, "No handlers for address "
                                                               + sendContext.message.address);
      }
    }
  }

  protected <T> boolean deliverMessageLocally(MessageImpl msg) {
    msg.setBus(this);
    Handlers handlers = handlerMap.get(msg.address());
    if (handlers != null) {
      if (msg.isSend()) {
        //Choose one
        HandlerHolder holder = handlers.choose();
        。。。
        if (holder != null) {
          deliverToHandler(msg, holder);
        }
      } else {
        // Publish
        。。。
        for (HandlerHolder holder: handlers.list) {
          deliverToHandler(msg, holder);
        }
      }
      return true;
    } else {
      。。。
      return false;
    }
  }
}

// EventBusImpl内部类
class SendContextImpl{

  public final Iterator<Handler<SendContext>> iter;

  public SendContextImpl(MessageImpl message, DeliveryOptions options, HandlerRegistration<T> handlerRegistration) {
	  this.message = message;
	  this.options = options;
	  this.handlerRegistration = handlerRegistration;
	  this.iter = interceptors.iterator();
  }

  public void next() {
  	  // 遍历所有的拦截器，依次对this进行处理
	  if (iter.hasNext()) {
	    Handler<SendContext> handler = iter.next();
	    try {
	      handler.handle(this);
	    } catch (Throwable t) {
	      log.error("Failure in interceptor", t);
	    }
	  } else {
	    sendOrPub(this);
	  }
  }

}

// handler与eventbus绑定，需要通过该对象
class HandlerRegistration<T>{

  private final Queue<Message<T>> pending = new ArrayDeque<>(8);

  private boolean registered;	//是否在eventbus上注册

  private Handler<Message<T>> handler;	//消息处理的handler

  public HandlerRegistration(Vertx vertx, EventBusMetrics metrics, EventBusImpl eventBus, String address,
                             String repliedAddress, boolean localOnly,
                             Handler<AsyncResult<Message<T>>> asyncResultHandler, long timeout) {
    this.vertx = vertx;
    this.metrics = metrics;
    this.eventBus = eventBus;
    this.address = address;
    this.repliedAddress = repliedAddress;
    this.localOnly = localOnly;
    this.asyncResultHandler = asyncResultHandler;
    if (timeout != -1) {
      timeoutID = vertx.setTimer(timeout, tid -> {
        if (metrics != null) {
          metrics.replyFailure(address, ReplyFailure.TIMEOUT);
        }
        sendAsyncResultFailure(ReplyFailure.TIMEOUT, "Timed out after waiting " + timeout + "(ms) for a reply. address: " + address + ", repliedAddress: " + repliedAddress);
      });
    }
  }

  public synchronized MessageConsumer<T> handler(Handler<Message<T>> handler) {
    this.handler = handler;
    if (this.handler != null && !registered) {
      registered = true;
      eventBus.addRegistration(address, this, repliedAddress != null, localOnly);
    } else if (this.handler == null && registered) {
      // This will set registered to false
      this.unregister();
    }
    return this;
  }

}