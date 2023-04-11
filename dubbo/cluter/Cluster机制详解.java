Cluster封装：
Cluster$Adapter{
	slot_4(MockClusterWrapper):{
		cluster(FailoverCluster)
	}
}


ReferenceConfig.init时会在Consumer端生成Stub对象：
class ReferenceConfig<T> {

	private void init() {
		..
		ref = createProxy(map);
	}

	private T createProxy(Map<String, String> map) {
		..
		invoker = refprotocol.refer(interfaceClass, urls.get(0));
        ..
        return (T) proxyFactory.getProxy(invoker);
	}
}
其中，invoker最终的实现类为：MockClusterInvoker，内部包含了FailoverCluserInvoker对象
FailoverCluserInvoker是一个虚拟Invoker，依靠RegistryDirectory对象，管理着注册中心某个具体目录下面，所有的provider url以及以此生成的Invoker实例。
当调用Invoker#invok()方法时，MockClusterInvoker会委托给FailoverCluserInvoker。FailoverCluserInvoker会从RegistryDirectory找出合适的Invoker进行调用，并会failover。


Cluster会根据具体某个目录，生成一个虚拟的Invoker，里面可能包含一个或多个真实的Invoker。调用该虚拟Invoker时，会根据负载均衡策略，找到一个真实Invoker进行调用，如果调用调用失败，则会根据策略进行下一步处理，如failover就选择另一个真实Invoker重试，failfast则直接失败。