class Http11NioProtocol 
	extends AbstractHttp11JsseProtocol<NioChannel> 
	extends AbstractProtocol implements ProtocolHandler{

	NioEndpoint endpoint;

	public Http11NioProtocol() {
        super(new NioEndpoint());
    }
    public AbstractHttp11Protocol(AbstractEndpoint<S> endpoint) {
        super(endpoint);
        setConnectionTimeout(Constants.DEFAULT_CONNECTION_TIMEOUT);
        ConnectionHandler<S> cHandler = new ConnectionHandler<>(this);
        setHandler(cHandler);
        getEndpoint().setHandler(cHandler);
    }
    public AbstractProtocol(AbstractEndpoint<S> endpoint) {
        this.endpoint = endpoint;
        setSoLinger(Constants.DEFAULT_CONNECTION_LINGER);
        setTcpNoDelay(Constants.DEFAULT_TCP_NO_DELAY);
    }

	// AbstractProtocol
	void init(){
		..
		endpoint.init();
	}
	// AbstractProtocol
	void start(){
		..
		endpoint.start();
		..
		timeoutThread.start();
	}

	// AbstractProtocol
	class ConnectionHandler{
		public SocketState process(SocketWrapperBase<S> wrapper, SocketEvent status){
			S socket = wrapper.getSocket();

            Processor processor = connections.get(socket);

            if (processor == null) {
                processor = getProtocol().createProcessor();
                register(processor);
            }

            connections.put(socket, processor);

            state = processor.process(wrapper, status);
		}
	}
}