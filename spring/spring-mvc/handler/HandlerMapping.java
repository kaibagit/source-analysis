public interface HandlerMapping {

	// 如果能够处理该request，则返回对象；否则返回null
	HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}