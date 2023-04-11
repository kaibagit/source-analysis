基于版本 3.5.1

启动Verticle流程：
->Vertx(VertxImpl)#deployVerticle(xxx)
	->DeploymentManager#deployVerticle(name, options, completionHandler)
		->DeploymentManager#doDeployVerticle(identifier, generateDeploymentID(), options, callingContext, callingContext, cl, completionHandler)
			->DeploymentManager#doDeployVerticle(iter, null, identifier, deploymentID, options, parentContext, callingContext, cl, completionHandler)
				->VerticleFactory(JavaVerticleFactory)#doDeploy(identifier, deploymentID, options, parentContext, callingContext, completionHandler, cl, verticles)
					->ContextImpl#runOnContext()
						->Verticle#start(startFuture)


同一个Verticle可以创建多个实例
每个Verticle实例对应一个EventLoopContext
一个或多个EventLoopContext对应一个EventLoop


监听端口，HttpServerImpl#listen()流程：
