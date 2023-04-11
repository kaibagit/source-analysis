class ServiceConfig<T>{

	private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();		//在原先Protocol的基础上，会封装一些wappers
	private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

	private Class<?> interfaceClass;
	// 接口实现类引用
    private T ref;


	public synchronized void export() {
		。。。
		doExport();
	}

	protected synchronized void doExport(){
		。。。
		doExportUrls();
	}

	private void doExportUrls() {
        List<URL> registryURLs = loadRegistries(true);
        for (ProtocolConfig protocolConfig : protocols) {
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
    	。。。
    	Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);		//默认返回AbstractProxyInvoker的子类

    	Exporter<?> exporter = protocol.export(invoker);
        exporters.add(exporter);
    	。。。
    }
}