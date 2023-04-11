service接口注册：
1、注册DubboNamespaceHandler处理dubbo标签
2、解析xml，用DubboBeanDefinitionParser生成BeanDefinition，其中的BeanClass为ServiceBean.class
根据BeanDefinition创建ServiceBean实例，触发afterPropertiesSet()方法，间接调用父类方法ServiceConfig#export()

reference依赖注册：
1、注册DubboNamespaceHandler处理reference标签
2、解析xml，用DubboBeanDefinitionParser生成BeanDefinition，其中BeanClass为ReferenceBean.class
根据BeanDefinition创建ReferenceBean实例，触发afterPropertiesSet()方法，间接调用父类方法ReferenceConfig#init()



//server端bind流程：
->ServiceConfig#export()
	->ServiceConfig#doExport()
		->ServiceConfig#doExportUrls()
			->ServiceConfig#doExportUrlsFor1Protocol(protocolConfig,registryURLs)
				->Protocol(DubboProtocol)#export(invoker)
					->DubboProtocol#openServer(url)
						->DubboProtocol#createServer(url)
							->Exchangers#bind(url, requestHandler)
								-->NettyTransporter#bind(url,channelHandler)
ServiceConfig#export()流程：
->ServiceConfig#export()
	->ServiceConfig#doExport()
		->ServiceConfig#doExportUrls()
			->ServiceConfig#doExportUrlsFor1Protocol(protocolConfig, registryURLs)
				->Protocol#export(invoker)

Protocol#export(invoker)流程：
->DubboProtocol#export(invoker)
	->DubboProtocol#openServer(url)

//查询激活的扩展：
->ExtensionLoader#getAdaptiveExtension()
	->ExtensionLoader#createAdaptiveExtension()

//内部调用：
->Invoker#invoke(invocation)
	->AbstractProxyInvoker#doInvoke(proxy,methodName,parameterTypes,arguments)
		->Wrapper#invokeMethod(proxy, methodName, parameterTypes, arguments);



//包含关系：
HeaderExchangeServer{
	NettyServer{
		DecodeHandler{
			HeaderExchangeHandler{
				Protocol实现类内置的handler
			}
		}
	}
}

//Provider接收request处理流程：
->NettyServerHandler
	->NettyServer
		->DecodeHandler
			->HeaderExchangeHandler
				->DubboProtocol.requestHandler
					->JavassistProxyFactory的AbstractProxyInvoker实现类
						->Wrapper动态生成的类
							->实现bean





