reference依赖注册：
1、注册DubboNamespaceHandler处理reference标签
2、解析xml，用DubboBeanDefinitionParser生成BeanDefinition，其中BeanClass为ReferenceBean.class
根据BeanDefinition创建ReferenceBean实例，触发afterPropertiesSet()方法，间接调用父类方法ReferenceConfig#init()


class ReferenceConfig<T> {

	// 默认dubbo协议，并在它外面包装了多层wapper
	private static final Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

	private void init() {
		..
		ref = createProxy(map);
	}

	private T createProxy(Map<String, String> map) {
		..
		// 当只存在一个注册中时，核心代码如下；urls里即为该注册中心的URL
		invoker = refprotocol.refer(interfaceClass, urls.get(0));
        ..
        // 创建服务代理
        return (T) proxyFactory.getProxy(invoker);
	}
}

ReferenceConfig#init()最终会调用到Protocol#refer()方法，而ReferenceConfig.refprotocol对象为一个Protocol链，其中就包括ProtocolFilterWrapper。

ProtocolFilterWrapper会根据URL加载所有Filter，将每个Filter封装成一个Invoker，最底层为真实的DubboInvoker，每个Filter的Invoker之间形成责任链，层次封装。