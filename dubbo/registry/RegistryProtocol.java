public class RegistryProtocol implements Protocol {

	private Cluster cluster;

	// 由Protocol$Adaptive在refer()时注入，实例为：Cluster$Adaptive
	public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    // 入参url范例：registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=dubbo_test_customer&dubbo=2.5.7&pid=6294&refer=application%3Ddubbo_test_customer%26dubbo%3D2.5.7%26interface%3Dcom.luliru.practice.api.provider.UserService%26methods%3DcreateDynamically%2CunstableHello%2CthrowInnerException%2Ccreate%2CthrowUnknownSubException%2Chello%2CfingByIds%26mock%3Dreturn%2Bnull%26pid%3D6294%26register.ip%3D192.168.96.107%26retries%3D0%26revision%3D1.0-SNAPSHOT%26side%3Dconsumer%26timestamp%3D1560913711626&registry=zookeeper&timestamp=1560913711680
	public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
		url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
		// 变更后url范例：zookeeper://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=dubbo_test_customer&dubbo=2.5.7&pid=6294&refer=application%3Ddubbo_test_customer%26dubbo%3D2.5.7%26interface%3Dcom.luliru.practice.api.provider.UserService%26methods%3DcreateDynamically%2CunstableHello%2CthrowInnerException%2Ccreate%2CthrowUnknownSubException%2Chello%2CfingByIds%26mock%3Dreturn%2Bnull%26pid%3D6294%26register.ip%3D192.168.96.107%26retries%3D0%26revision%3D1.0-SNAPSHOT%26side%3Dconsumer%26timestamp%3D1560913711626&timestamp=1560913711680
		Registry registry = registryFactory.getRegistry(url);
		..
		return doRefer(cluster, registry, type, url);
	}

	private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
		RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
		..
		URL subscribeUrl = ..
		..
		directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY, 
                Constants.PROVIDERS_CATEGORY 
                + "," + Constants.CONFIGURATORS_CATEGORY 
                + "," + Constants.ROUTERS_CATEGORY));
		return cluster.join(directory);
	}

	public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
		// originInvoker的ulr范例：
		// registry://192.168.11.29:2181/com.alibaba.dubbo.registry.RegistryService?application=merchant-mealdone-service&backup=192.168.11.32:2181,192.168.11.20:2181&dubbo=2.5.6&export=dubbo%3A%2F%2F192.168.96.107%3A12100%2Fcom.dianwoda.merchant.mealdone.api.provider.MealdoneProvider%3Fanyhost%3Dtrue%26application%3Dmerchant-mealdone-service%26default.service.filter%3DTraceAccessFilter%2Cdefault%2CDubboAccessLogFilter%26dubbo%3D2.5.6%26generic%3Dfalse%26interface%3Dcom.dianwoda.merchant.mealdone.api.provider.MealdoneProvider%26methods%3DriderReportFake%2CgetMealdoneOrder%2CgetMealdoneOrders%2CsavePredictMeadDoneTime%2CmerchantMarkMealdone%26monitorLog%3Dtrue%26pid%3D78162%26side%3Dprovider%26threadpool%3DMonitorableFixed%26threads%3D500%26timestamp%3D1558677456674&pid=78162&registry=zookeeper&timestamp=1558677441453
		..
		final Registry registry = getRegistry(originInvoker);
		//registedProviderUrl范例：dubbo://192.168.96.107:12100/com.dianwoda.merchant.mealdone.api.provider.MealdoneProvider?anyhost=true&application=merchant-mealdone-service&default.service.filter=TraceAccessFilter,default,DubboAccessLogFilter&dubbo=2.5.6&generic=false&interface=com.dianwoda.merchant.mealdone.api.provider.MealdoneProvider&methods=riderReportFake,getMealdoneOrder,getMealdoneOrders,savePredictMeadDoneTime,merchantMarkMealdone&monitorLog=true&pid=78162&side=provider&threadpool=MonitorableFixed&threads=500&timestamp=1558677456674
        final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);
        registry.register(registedProviderUrl);
        ..
	}
}