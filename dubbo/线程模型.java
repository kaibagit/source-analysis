interface Dispatcher{
	// 返回处理消息的ChannelHandler
	ChannelHandler dispatch(ChannelHandler handler, URL url);
}

// 处理channel消息
interface ChannelHandler{
	void connected(Channel channel);
	void disconnected(Channel channel);
	void sent(Channel channel, Object message);
	void received(Channel channel, Object message);
	void caught(Channel channel, Throwable exception);
}

public class DirectDispatcher implements Dispatcher {
    public ChannelHandler dispatch(ChannelHandler handler, URL url) {
        return handler;		//不转交给额外的线程池处理
    }
}

class AllChannelHandler{
	// WrappedChannelHandler
	ExecutorService executor;	//将所有的消息提交给executor处理
}

class ConnectionOrderedChannelHandler{
	ThreadPoolExecutor connectionExecutor;  //单线程，用来处理connected、disconnected事件，其他交由executor处理
}

class ExecutionChannelHandler{
	//除了sent，其他都交由executor处理
}

class MessageOnlyChannelHandler{
	//只有received，交由executor处理
}


//netty3
class NettyHandler{
	//类似于NettyServerHandler
}


// netty4
class NettyServer{
	// 继承自AbstractPeer
	ChannelHandler handler;		//dubbo的ChannelHandler，且是通过Dispatcher封装后的ChannelHandler

	public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));	//间接会通过Dispatcher拿到封装后的ChannelHandler
    }

    //间接实现了dubbo的ChannelHandler，所有事件交由handler属性处理
}
class NettyServerHandler extends ChannelDuplexHandler{	//继承自netty的ChannelHandler
	//通过NettyChannel.getOrAddChannel()，将netty的channel封装成dubbo的channel，交由dubbo的ChannelHandler处理，即NettyServer处理
	//netty事件与dubbo事件映射：
	//channelActive => connected
	//channelInactive => disconnected
	//disconnect => 不处理
	//channelRead => received
	//write => sent
	//exceptionCaught => caught
}