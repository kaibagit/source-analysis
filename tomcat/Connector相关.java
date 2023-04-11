class Connector{

	protected final ProtocolHandler protocolHandler;

	public Connector(String protocol) {
		..
		Class<?> clazz = Class.forName(protocolHandlerClassName);	//protocolHandlerClassName="org.apache.coyote.http11.Http11NioProtocol"
        p = (ProtocolHandler) clazz.getConstructor().newInstance();
        ..
        this.protocolHandler = p;
        ..
	}

	void initInternal(){
		super.initInternal();
		..
		protocolHandler.init();
		..
	}
	void startInternal(){
		..
		protocolHandler.start();
		..
	}
}








