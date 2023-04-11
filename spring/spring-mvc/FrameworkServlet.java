public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

	protected final void doGet(HttpServletRequest request, HttpServletResponse response) {
this.processRequest(request, response);
}

protected final void processRequest(HttpServletRequest request, HttpServletResponse response) {
	..
	this.doService(request, response);
	..
}

protected abstract void doService(HttpServletRequest var1, HttpServletResponse var2);
}