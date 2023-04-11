class NamesrvStartup{

    public static NamesrvController main0(String[] args) {
    	。。。
        NamesrvController controller = createNamesrvController(args);
        start(controller);
        。。。
    }

    public static NamesrvController createNamesrvController(String[] args) throws IOException, JoranException {
        。。。

        final NamesrvController controller = new NamesrvController(namesrvConfig, nettyServerConfig);

        // remember all configs to prevent discard
        controller.getConfiguration().registerConfig(properties);

        return controller;
    }

    public static NamesrvController start(final NamesrvController controller) throws Exception {
    	。。。

        boolean initResult = controller.initialize();
        。。。

        // 注册ShutdownHook

        controller.start();

        return controller;
    }
}


class NamesrvController{

	private RemotingServer remotingServer;

	public NamesrvController(NamesrvConfig namesrvConfig, NettyServerConfig nettyServerConfig) {
        。。。
        this.brokerHousekeepingService = new BrokerHousekeepingService(this);
        。。。
    }

	public boolean initialize() {
		。。。
		this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.brokerHousekeepingService);
		。。。
		this.registerProcessor();
		。。。
	}

	public void start() throws Exception {
        this.remotingServer.start();

        if (this.fileWatchService != null) {
            this.fileWatchService.start();
        }
    }

    // 向NettyRemotingServer注册Processor
    private void registerProcessor() {
        if (namesrvConfig.isClusterTest()) {
            this.remotingServer.registerDefaultProcessor(new ClusterTestRequestProcessor(this, namesrvConfig.getProductEnvName()),
                this.remotingExecutor);
        } else {
            this.remotingServer.registerDefaultProcessor(new DefaultRequestProcessor(this), this.remotingExecutor);
        }
    }
}