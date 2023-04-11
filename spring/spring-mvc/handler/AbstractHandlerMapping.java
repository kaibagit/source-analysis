public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport implements HandlerMapping, Ordered {

	// Look up a handler for the given request, falling back to the default handler if no specific one is found.
	public final HandlerExecutionChain getHandler(HttpServletRequest request){
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		..
	}

	protected abstract Object getHandlerInternal(HttpServletRequest request);
}