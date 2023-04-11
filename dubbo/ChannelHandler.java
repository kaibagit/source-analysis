public interface ChannelHandler {

    void connected(Channel channel) throws RemotingException;

    void disconnected(Channel channel) throws RemotingException;

    void sent(Channel channel, Object message) throws RemotingException;

    void received(Channel channel, Object message) throws RemotingException;

    void caught(Channel channel, Throwable exception) throws RemotingException;

}

public interface ChannelHandlerDelegate extends ChannelHandler {
    public ChannelHandler getHandler();
}


// DubboProtocol内部对象
private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {

    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof Invocation) {
            reply((ExchangeChannel) channel, message);
        } else {
            super.received(channel, message);
        }
    }
    
};

class HeaderExchangeHandler{

	public HeaderExchangeHandler(ExchangeHandler handler) {
        。。。
        this.handler = handler;
    }

}

class DecodeHandler{

	// AbstractChannelHandlerDelegate
	protected ChannelHandler handler;

	public DecodeHandler(ChannelHandler handler) {
        super(handler);
    }

    // AbstractChannelHandlerDelegate
    protected AbstractChannelHandlerDelegate(ChannelHandler handler) {
        。。。
        this.handler = handler;
    }
}