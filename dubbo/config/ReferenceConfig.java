class ReferenceConfig<T> {

	//注册中心URL列表，一个zk集群算一项
	//范例：registry://192.168.11.29:2285/com.alibaba.dubbo.registry.RegistryService?application=sparta-gw&backup=192.168.11.32:2285,192.168.11.20:2285&dubbo=2.5.6.2-D-RELEASE&pid=26306&refer=application%3Dsparta-gw%26default.check%3Dfalse%26default.reference.filter%3Ddefault%2CTraceInvokeFilter%2CDubboInvokeLogFilter%26default.retries%3D0%26dubbo%3D2.5.6.2-D-RELEASE%26interface%3Dcom.dianwoba.optimus.basedata.provider.CityQueryProvider%26methods%3DgetAllCityList%2CgetAllCityListSortByPinyin%2CgetCityByName%2CgetCityById%2CgetCityByCode%2CgetCityByAreaCode%2CgetAllCityListBySort%26monitor%3Ddubbo%253A%252F%252F192.168.11.29%253A2285%252Fcom.alibaba.dubbo.registry.RegistryService%253Fapplication%253Dsparta-gw%2526backup%253D192.168.11.32%253A2285%252C192.168.11.20%253A2285%2526dubbo%253D2.5.6.2-D-RELEASE%2526pid%253D26306%2526protocol%253Dregistry%2526refer%253Ddubbo%25253D2.5.6.2-D-RELEASE%252526interface%25253Dcom.alibaba.dubbo.monitor.MonitorService%252526pid%25253D26306%252526timestamp%25253D1560823982654%2526registry%253Dzookeeper%2526timestamp%253D1560823982644%26pid%3D26306%26retries%3D1%26revision%3D1.0.1-SNAPSHOT%26route.server%3Dfalse%26side%3Dconsumer%26timestamp%3D1560823982610%26version%3D1.0.0&registry=zookeeper&timestamp=1560823982644
	private final List<URL> urls = new ArrayList<URL>();

	// 默认dubbo协议，并在它外面包装了多层wapper
	private static final Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

	// 默认FailoverCluster
	private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

	private transient volatile Invoker<?> invoker;

	private void init() {
		..
		ref = createProxy(map);
	}

	private T createProxy(Map<String, String> map) {
		..
		if (isJvmRefer) {
            ..
        } else {
        	if (url != null && url.length() > 0) { // 用户指定URL，指定的URL可能是对点对直连地址，也可能是注册中心URL
                ..
            } else { // 通过注册中心配置拼装URL
            	List<URL> us = loadRegistries(false);	//us为注册中心的URL列表
            	..
            	for (URL u : us) {
            	    ..
            	    urls.add(u.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                }
                ..
                if (urls.size() == 1) {
                	invoker = refprotocol.refer(interfaceClass, urls.get(0));
	            } else {
	                ..
	                invoker = cluster.join(new StaticDirectory(u, invokers));
	                ..
	            }
            }
        }
        ..
        // 创建服务代理
        // invoker最终实现类为MockClusterInvoker
        return (T) proxyFactory.getProxy(invoker);
	}
}


MockClusterInvoker{
	directory(RegistryDirectory)
	invoker(FailoverCluserInvoker):{
		directory(RegistryDirectory)
		当调用invoke()方式时:{
			List<Invoker<T>>:[
				RegistryDirectory$InvokerDelegete{
					invoker(ListenerInvokerWrapper):{
						invoker(ProtocolFilterWrapper):{
							invoker(DubboInvoker)
							next(ProtocolFilterWrapper)
						}
					}
				}
			]
		}
	}
}