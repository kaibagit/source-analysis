public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

	// 该方法继承自AbstractClusterInvoker
	public Result invoke(final Invocation invocation) throws RpcException {
		..
		// 从Directory获取所有符合的Invoker
		List<Invoker<T>> invokers = list(invocation);
        ..
        return doInvoke(invocation, invokers, loadbalance);
    }

    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
    	..
    	// 尝试调用次数
    	int len = getUrl().getMethodParameter(invocation.getMethodName(), Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES) + 1;
    	..
    	List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(copyinvokers.size());	//已经被调用过的Invoker
    	..
    	for (int i = 0; i < len; i++) {
            ..
            Invoker<T> invoker = select(loadbalance, invocation, copyinvokers, invoked);
            invoked.add(invoker);
            RpcContext.getContext().setInvokers((List) invoked);
            try {
                Result result = invoker.invoke(invocation);
                ..
                return result;
            } catch (RpcException e) {
	            if (e.isBiz()) { // biz exception.
	                throw e;
	            }
	            le = e;
	        }
            ..
        }
    }
}