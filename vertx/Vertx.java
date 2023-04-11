基于版本 3.5.1

class VertxImpl{
  public void deployVerticle(String name, 
                        DeploymentOptions options{default:new DeploymentOptions()}, 
                        Handler<AsyncResult<String>> completionHandler{default:null}) {
    if (options.isHa() && haManager() != null && haManager().isEnabled()) {
      haManager().deployVerticle(name, options, completionHandler);
    } else {
      deploymentManager.deployVerticle(name, options, completionHandler);
    }
  }

  public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered,
                                  Handler<AsyncResult<T>> asyncResultHandler) {
    ContextImpl context = getOrCreateContext();
    context.executeBlocking(blockingCodeHandler, ordered, asyncResultHandler);
  }

  public ContextImpl getOrCreateContext() {
    ContextImpl ctx = getContext();
    if (ctx == null) {
      // We are running embedded - Create a context
      ctx = createEventLoopContext(null, null, new JsonObject(), Thread.currentThread().getContextClassLoader());
    }
    return ctx;
  }

  public EventLoopContext createEventLoopContext(String deploymentID, WorkerPool workerPool, JsonObject config, ClassLoader tccl) {
    return new EventLoopContext(this, internalBlockingPool, workerPool != null ? workerPool : this.workerPool, deploymentID, config, tccl);
  }

  public ContextImpl createWorkerContext(boolean multiThreaded, String deploymentID, WorkerPool workerPool, JsonObject config,
                                         ClassLoader tccl) {
    if (workerPool == null) {
      workerPool = this.workerPool;
    }
    if (multiThreaded) {
      return new MultiThreadedWorkerContext(this, internalBlockingPool, workerPool, deploymentID, config, tccl);
    } else {
      return new WorkerContext(this, internalBlockingPool, workerPool, deploymentID, config, tccl);
    }
  }
}