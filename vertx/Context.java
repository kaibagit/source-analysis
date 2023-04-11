基于版本 3.5.1

class ContextImpl{
	public <T> void executeBlocking(Action<T> action, Handler<AsyncResult<T>> resultHandler) {
    	executeBlocking(action, null, resultHandler, internalBlockingPool.executor(), internalOrderedTasks, internalBlockingPool.metrics());
	}
	public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler) {
    	executeBlocking(null, blockingCodeHandler, resultHandler, workerPool.executor(), ordered ? orderedTasks : null, workerPool.metrics());
  	}

	<T> void executeBlocking(Action<T> action, Handler<Future<T>> blockingCodeHandler,
	      Handler<AsyncResult<T>> resultHandler,
	      Executor exec, TaskQueue queue, PoolMetrics metrics) {
	    。。。
	    try {

	      Runnable command = () -> {
	        VertxThread current = (VertxThread) Thread.currentThread();
	        。。。
	        Future<T> res = Future.future();
	        try {
	          if (blockingCodeHandler != null) {
	            ContextImpl.setContext(this);
	            blockingCodeHandler.handle(res);
	          } else {
	            T result = action.perform();
	            res.complete(result);
	          }
	        } catch (Throwable e) {
	          res.fail(e);
	        } finally {
	          。。。
	        }
	        。。。
	        if (resultHandler != null) {
	          runOnContext(v -> res.setHandler(resultHandler));
	        }
	      };


	      if (queue != null) {
	        queue.execute(command, exec);
	      } else {
	        exec.execute(command);
	      }


	    } catch (RejectedExecutionException e) {
	      。。。
	    }
	}

	public void runOnContext(Handler<Void> task) {
	    try {
	      executeAsync(task);	//子类实现
	    } catch (RejectedExecutionException ignore) {
	      // Pool is already shut down
	    }
	}
}

class EventLoopContext extends ContextImpl{

  public EventLoopContext(VertxInternal vertx, WorkerPool internalBlockingPool, WorkerPool workerPool, String deploymentID, JsonObject config,
                          ClassLoader tccl) {
    super(vertx, internalBlockingPool, workerPool, deploymentID, config, tccl);
  }

  protected ContextImpl(VertxInternal vertx, WorkerPool internalBlockingPool, WorkerPool workerPool, String deploymentID, JsonObject config,
                        ClassLoader tccl) {
    this(vertx, getEventLoop(vertx), internalBlockingPool, workerPool, deploymentID, config, tccl);
  }

  protected ContextImpl(VertxInternal vertx, EventLoop eventLoop, WorkerPool internalBlockingPool, WorkerPool workerPool, String deploymentID, JsonObject config,
                        ClassLoader tccl) {
    this.deploymentID = deploymentID;
    this.config = config;
    this.eventLoop = eventLoop;
    this.tccl = tccl;
    this.owner = vertx;
    this.workerPool = workerPool;
    this.internalBlockingPool = internalBlockingPool;
    this.orderedTasks = new TaskQueue();
    this.internalOrderedTasks = new TaskQueue();
    this.closeHooks = new CloseHooks(log);
  }

  public void executeAsync(Handler<Void> task) {
    // No metrics, we are on the event loop.
    nettyEventLoop().execute(wrapTask(null, task, true, null));		//调用netty的EventLoop执行
  }
}

class WorkerContext extends ContextImpl{
  
  public WorkerContext(VertxInternal vertx, WorkerPool internalBlockingPool, WorkerPool workerPool, String deploymentID,
                       JsonObject config, ClassLoader tccl) {
    super(vertx, internalBlockingPool, workerPool, deploymentID, config, tccl);
  }

  public void executeAsync(Handler<Void> task) {
    orderedTasks.execute(wrapTask(null, task, true, workerPool.metrics()), workerPool.executor());
  }

}




