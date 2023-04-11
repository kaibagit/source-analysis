public class DispatcherServlet extends FrameworkServlet {

	// 实现为普通的ArrayList，初始化时，会对HandlerMapping经过Order注解值排序
	private List<HandlerMapping> handlerMappings;


	private void initHandlerMappings(ApplicationContext context) {
		..
		Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
		..
		this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
		// We keep HandlerMappings in sorted order.
		AnnotationAwareOrderComparator.sort(this.handlerMappings);
		..
	}

	

	protected void doService(HttpServletRequest request, HttpServletResponse response){
		..
		this.doDispatch(request, response);
		..
	}

	protected void doDispatch(HttpServletRequest request, HttpServletResponse response){
		HttpServletRequest processedRequest = request;
HandlerExecutionChain mappedHandler = null;
..
ModelAndView mv = null;
		..
		mappedHandler = this.getHandler(processedRequest);
		// 响应404
if (mappedHandler == null || mappedHandler.getHandler() == null) {
this.noHandlerFound(processedRequest, response);
return;
}

// Determine handler adapter for the current request.
		HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
		..
		// Actually invoke the handler.
		mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
		..
	}

	protected HandlerExecutionChain getHandler(HttpServletRequest request){
		for (HandlerMapping hm : this.handlerMappings) {
			..
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}
}