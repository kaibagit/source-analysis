基于版本 3.5.1

class DeploymentManager{
  public void deployVerticle(String identifier,
                             DeploymentOptions options,
                             Handler<AsyncResult<String>> completionHandler) {
    if (options.isMultiThreaded() && !options.isWorker()) {
      throw new IllegalArgumentException("If multi-threaded then must be worker too");
    }
    ContextImpl callingContext = vertx.getOrCreateContext();
    ClassLoader cl = getClassLoader(options, callingContext);
    doDeployVerticle(identifier, generateDeploymentID(), options, callingContext, callingContext, cl, completionHandler);
  }

  private void doDeployVerticle(String identifier,
                                String deploymentID,
                                DeploymentOptions options,
                                ContextImpl parentContext,
                                ContextImpl callingContext,
                                ClassLoader cl,
                                Handler<AsyncResult<String>> completionHandler) {
    List<VerticleFactory> verticleFactories = resolveFactories(identifier);
    Iterator<VerticleFactory> iter = verticleFactories.iterator();
    doDeployVerticle(iter, null, identifier, deploymentID, options, parentContext, callingContext, cl, completionHandler);
  }

  private void doDeployVerticle(Iterator<VerticleFactory> iter,
                                Throwable prevErr,
                                String identifier,
                                String deploymentID,
                                DeploymentOptions options,
                                ContextImpl parentContext,
                                ContextImpl callingContext,
                                ClassLoader cl,
                                Handler<AsyncResult<String>> completionHandler) {
    // 遍历VerticleFactory列表，一般只有JavaVerticleFactory一个实现
    if (iter.hasNext()) {
      VerticleFactory verticleFactory = iter.next();

      Future<String> fut = Future.future();

      if (verticleFactory.requiresResolve()) {
        。。。
      } else {
        fut.complete(identifier);
      }

      fut.setHandler(ar -> {

        Throwable err;

        if (ar.succeeded()) {
          String resolvedName = ar.result();
          // 如果在resolve过程中，identifier发生了变更，则重新deployVerticle
          if (!resolvedName.equals(identifier)) {
            try {
              deployVerticle(resolvedName, options, completionHandler);
            } catch (Exception e) {
              completionHandler.handle(Future.failedFuture(e));
            }
            return;
          } else {
            if (verticleFactory.blockingCreate()) {
              。。。
              return;
            } else {
              try {
                // 实例化指定数量的Verticle
                Verticle[] verticles = createVerticles(verticleFactory, identifier, options.getInstances(), cl);
                // 最终deploy
                doDeploy(identifier, deploymentID, options, parentContext, callingContext, completionHandler, cl, verticles);
                return;
              } catch (Exception e) {
                err = e;
              }
            }
          }
        } else {
          err = ar.cause();
        }
        // Try the next one
        doDeployVerticle(iter, err, identifier, deploymentID, options, parentContext, callingContext, cl, completionHandler);
      });
    } else {
      。。。
    }
  }

  private Verticle[] createVerticles(VerticleFactory verticleFactory, String identifier, 
                                    int instances,  //创建的Verticle数量
                                    ClassLoader cl) {
    Verticle[] verticles = new Verticle[instances];
    for (int i = 0; i < instances; i++) {
      verticles[i] = verticleFactory.createVerticle(identifier, cl);
      if (verticles[i] == null) {
        throw new NullPointerException("VerticleFactory::createVerticle returned null");
      }
    }
    return verticles;
  }

  private void doDeploy(String identifier, String deploymentID, DeploymentOptions options,
                        ContextImpl parentContext,
                        ContextImpl callingContext,
                        Handler<AsyncResult<String>> completionHandler,
                        ClassLoader tccl, Verticle... verticles) {
    JsonObject conf = options.getConfig() == null ? new JsonObject() : options.getConfig().copy(); // Copy it
    String poolName = options.getWorkerPoolName();

    Deployment parent = parentContext.getDeployment();
    DeploymentImpl deployment = new DeploymentImpl(parent, deploymentID, identifier, options);

    AtomicInteger deployCount = new AtomicInteger();
    AtomicBoolean failureReported = new AtomicBoolean();
    for (Verticle verticle: verticles) {
      // 如果配置了workerPoolName，则使用该共享的线程池
      WorkerExecutorImpl workerExec = poolName != null ? vertx.createSharedWorkerExecutor(poolName, options.getWorkerPoolSize(), options.getMaxWorkerExecuteTime()) : null;
      WorkerPool pool = workerExec != null ? workerExec.getPool() : null;

      // 创建Context
      ContextImpl context = options.isWorker() ? vertx.createWorkerContext(options.isMultiThreaded(), deploymentID, pool, conf, tccl) :
        vertx.createEventLoopContext(deploymentID, pool, conf, tccl);
      if (workerExec != null) {
        context.addCloseHook(workerExec);
      }
      context.setDeployment(deployment);

      // DeploymentImpl增加Verticle
      deployment.addVerticle(new VerticleHolder(verticle, context));

      // context启动Verticle
      context.runOnContext(v -> {
        try {
          // 初始化
          verticle.init(vertx, context);

          Future<Void> startFuture = Future.future();

          // 启动
          verticle.start(startFuture);

          startFuture.setHandler(ar -> {
            if (ar.succeeded()) {
              if (parent != null) {
                if (parent.addChild(deployment)) {
                  deployment.child = true;
                } else {
                  // Orphan
                  deployment.undeploy(null);
                  return;
                }
              }
              VertxMetrics metrics = vertx.metricsSPI();
              if (metrics != null) {
                metrics.verticleDeployed(verticle);
              }
              deployments.put(deploymentID, deployment);
              if (deployCount.incrementAndGet() == verticles.length) {
                reportSuccess(deploymentID, callingContext, completionHandler);
              }
            } else if (failureReported.compareAndSet(false, true)) {
              deployment.rollback(callingContext, completionHandler, context, ar.cause());
            }
          });
        } catch (Throwable t) {
          if (failureReported.compareAndSet(false, true))
            deployment.rollback(callingContext, completionHandler, context, t);
        }
      });
    }
  }
}




// 主要用于多语言支持
interface VerticleFactory{

  // 是否要求resolve
  default boolean requiresResolve() {
    return false;
  }

  // 创建Verticle是否会阻塞event loop
  default boolean blockingCreate() {
    return false;
  }

  default void resolve(String identifier, DeploymentOptions deploymentOptions, ClassLoader classLoader, Future<String> resolution) {
    resolution.complete(identifier);
  }
}

class JavaVerticleFactory extends VerticleFactory(){

  public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
    verticleName = VerticleFactory.removePrefix(verticleName);
    Class clazz;
    if (verticleName.endsWith(".java")) {
      CompilingClassLoader compilingLoader = new CompilingClassLoader(classLoader, verticleName);
      String className = compilingLoader.resolveMainClassName();
      clazz = compilingLoader.loadClass(className);
    } else {
      clazz = classLoader.loadClass(verticleName);
    }
    return (Verticle) clazz.newInstance();
  }
}





