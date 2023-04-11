class Tomcat{
	protected Server server;

	public void start(){
        getServer();
        getConnector();
        server.start();
    }

    public Server getServer() {
    	server = new StandardServer();
    	..
    	Service service = new StandardService();
        service.setName("Tomcat");
        server.addService(service);
        return server;
    }

    public Connector getConnector() {
    	..
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(port);
        service.addConnector(connector);	//service为getServer().findServices()[0]，即StandardService实例
        defaultConnectorCreated = true;
        return connector;
    }
}










class StandardServer implements Server{

	protected void initInternal(){
		super.initInternal();
		..
		for (int i = 0; i < services.length; i++) {
            services[i].init();
        }
	}

	protected void startInternal(){
		..
		for (int i = 0; i < services.length; i++) {
            services[i].start();
        }
        ..
	}
}

















class StandardService implements Service{

	protected Connector connectors[];

	// Tomcat.start()时，会将Connection加入进入
	public void addConnector(Connector connector) {
		//将connector加入到connectors[]中
	}

	void initInternal(){
		super.initInternal();
		..
		engine.init();
		..
		connectors.each()->init();
	}

	void startInternal(){
		..
		engine.start();
		..
		connectors.each()->start();
		..
	}
}


















