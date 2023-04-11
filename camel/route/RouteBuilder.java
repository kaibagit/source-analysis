基于版本：2.22.0

class RouteBuilder{

	// BuilderSupport
	private ModelCamelContext context;

    RestsDefinition restCollection = new RestsDefinition();     // rest 定义集合
	RoutesDefinition routeCollection = new RoutesDefinition();	//一系列route的定义

	public RouteDefinition from(String uri) {
        getRouteCollection().setCamelContext(getContext());
        RouteDefinition answer = getRouteCollection().from(uri);
        configureRoute(answer);		//默认为空方法
        return answer;
    }

    // 获取当前的context，如果没有，则new DefaultCamelContext()
    public ModelCamelContext getContext() {
        ModelCamelContext context = super.getContext();
        if (context == null) {
            context = createContainer();
            setContext(context);
        }
        return context;
    }



    // DefaultCamelContext#addRoutes(builder)触发
    public void addRoutesToCamelContext(CamelContext context) throws Exception {
        // must configure routes before rests
        configureRoutes((ModelCamelContext) context);   //将context注入到routeCollection
        configureRests((ModelCamelContext) context);

        // but populate rests before routes, as we want to turn rests into routes
        populateRests();
        populateTransformers();
        populateValidators();
        populateRoutes();
    }
    public RoutesDefinition configureRoutes(ModelCamelContext context) throws Exception {
        setContext(context);
        checkInitialized();
        routeCollection.setCamelContext(context);
        return routeCollection;
    }
    public RestsDefinition configureRests(ModelCamelContext context) throws Exception {
        setContext(context);
        restCollection.setCamelContext(context);
        return restCollection;
    }
    protected void populateRoutes() throws Exception {
        ModelCamelContext camelContext = getContext();
        。。。
        getRouteCollection().setCamelContext(camelContext);
        camelContext.addRouteDefinitions(getRouteCollection().getRoutes());
    }
}



