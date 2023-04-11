基于版本：2.5.7

public interface Invoker<T> extends Node {

    Class<T> getInterface();

    Result invoke(Invocation invocation) throws RpcException;

}

class AbstractProxyInvoker{

	private final T proxy;

    private final Class<T> type;

    private final URL url;

	public AbstractProxyInvoker(T proxy, Class<T> type, URL url) {
		。。。
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }

    public Result invoke(Invocation invocation) throws RpcException {
        。。。
        return new RpcResult(doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments()));
        。。。
    }

    protected abstract Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable;
}