// Handler execution chain, consisting of handler object and any handler interceptors.
public class HandlerExecutionChain {

	private final Object handler;

	private HandlerInterceptor[] interceptors;

	private List<HandlerInterceptor> interceptorList;
}