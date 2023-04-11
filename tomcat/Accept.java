// NioEndpoint内部类
class Acceptor extends AbstractEndpoint.Acceptor{

	void run(){
		..
		while (running) {
			..
			SocketChannel socket = serverSock.accept();		//NioEndpoint.serverSock
			..
			setSocketOptions(socket);
			..
    	}
	}

	protected boolean setSocketOptions(SocketChannel socket) {
		..
		socket.configureBlocking(false);
        Socket sock = socket.socket();
        socketProperties.setProperties(sock);
        ..
        NioChannel channel = new NioChannel(socket, bufhandler);
        ..
        getPoller0().register(channel);
        ..
	}

}