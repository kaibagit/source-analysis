// 代指注册中心的某个目录
// 会监听某个目录的变更，以及生成和缓存相应的Invoker
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

	private Protocol protocol;

	// 用于缓存生成的Invoker
	// key值为provider端完整URL
	// key范例：dubbo://192.168.96.107:20880/com.luliru.practice.api.provider.UserService?anyhost=true&application=dubbo_test_customer&check=false&default.accesslog=logs/rpc_access.log&default.exceptionLog=true&dubbo=2.5.7&generic=false&interface=com.luliru.practice.api.provider.UserService&methods=unstableHello,createDynamically,throwInnerException,create,throwUnknownSubException,hello,fingByIds&mock=return+null&pid=98977&register.ip=192.168.96.107&remote.timestamp=1560848454414&retries=0&revision=1.0-SNAPSHOT&side=consumer&timestamp=1560911457895
	private volatile Map<String, Invoker<T>> urlInvokerMap;

	// 由RegistryProtocol注入，实现类为：Protocol$Adaptive
	public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    // 
    // invokerUrls范例：dubbo://192.168.96.107:20880/com.luliru.practice.api.provider.UserService?anyhost=true&application=dubbo_test_provider&default.accesslog=logs/rpc_access.log&default.corethreads=1&default.exceptionLog=true&default.threadpool=cached&default.threads=10&dubbo=2.5.7&generic=false&interface=com.luliru.practice.api.provider.UserService&methods=unstableHello,createDynamically,throwInnerException,create,throwUnknownSubException,hello,fingByIds&pid=3517&revision=1.0-SNAPSHOT&side=provider&timestamp=1560848454414
    private void refreshInvoker(List<URL> invokerUrls) {
    	..
    	Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);// 将URL列表转成Invoker列表
    	..
    	this.urlInvokerMap = newUrlInvokerMap;
    	..
    }

	// 根据url，生成Invoker
	// url范例：dubbo://192.168.96.107:20880/com.luliru.practice.api.provider.UserService?anyhost=true&application=dubbo_test_provider&default.accesslog=logs/rpc_access.log&default.corethreads=1&default.exceptionLog=true&default.threadpool=cached&default.threads=10&dubbo=2.5.7&generic=false&interface=com.luliru.practice.api.provider.UserService&methods=unstableHello,createDynamically,throwInnerException,create,throwUnknownSubException,hello,fingByIds&pid=3517&revision=1.0-SNAPSHOT&side=provider&timestamp=1560848454414
	private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
		Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<String, Invoker<T>>();
		..
		for (URL providerUrl : urls) {
			..
			URL url = mergeUrl(providerUrl);
            String key = url.toFullString();
            ..
			Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);
			..
			if (invoker == null) {
				..
				invoker = new InvokerDelegete<T>(protocol.refer(serviceType, url), url, providerUrl);
				..
			}
			..
			newUrlInvokerMap.put(key, invoker);
		}
		..
		return newUrlInvokerMap;
	}
}


RegistryProtocol#refer
	-> RegistryDirectory#subscribe
		-> ZookeeperRegistry#subscribe
			-> FailbackRigistry(ZookeeperRegistry)#doSubscribe
				-> FailbackRigistry(ZookeeperRegistry)#notify
					-> FailbackRigistry(ZookeeperRegistry)#doNotify
						-> AbstractRegistry(ZookeeperRegistry)#notify
							-> RegistryDirectory#notify
								-> RegistryDirectory#refreshInvoker