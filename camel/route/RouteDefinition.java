// 单个route的定义
class RouteDefinition extends ProcessorDefinition<RouteDefinition>{
	ModelCamelContext camelContext;

	private List<FromDefinition> inputs = new ArrayList<>();
    private List<ProcessorDefinition<?>> outputs = new ArrayList<>();

	public RouteDefinition from(@AsEndpointUri String uri) {
        getInputs().add(new FromDefinition(uri));
        return this;
    }
    
    public void prepare(ModelCamelContext context) {
        if (prepared.compareAndSet(false, true)) {
            RouteDefinitionHelper.prepareRoute(context, this);
        }
    }
}











class RouteDefinitionHelper{
    public static void prepareRoute(ModelCamelContext context, RouteDefinition route) {
        prepareRoute(context, route, null, null, null, null, null);
    }
    public static void prepareRoute(ModelCamelContext context, RouteDefinition route,
                                    List<OnExceptionDefinition> onExceptions,
                                    List<InterceptDefinition> intercepts,
                                    List<InterceptFromDefinition> interceptFromDefinitions,
                                    List<InterceptSendToEndpointDefinition> interceptSendToEndpointDefinitions,
                                    List<OnCompletionDefinition> onCompletions) {

        Runnable propertyPlaceholdersChangeReverter = ProcessorDefinitionHelper.createPropertyPlaceholdersChangeReverter();
        try {
            prepareRouteImp(context, route, onExceptions, intercepts, interceptFromDefinitions, interceptSendToEndpointDefinitions, onCompletions);
        } finally {
            // Lets restore
            propertyPlaceholdersChangeReverter.run();
        }
    }
    private static void prepareRouteImp(ModelCamelContext context, RouteDefinition route,
                                    List<OnExceptionDefinition> onExceptions,
                                    List<InterceptDefinition> intercepts,
                                    List<InterceptFromDefinition> interceptFromDefinitions,
                                    List<InterceptSendToEndpointDefinition> interceptSendToEndpointDefinitions,
                                    List<OnCompletionDefinition> onCompletions) {

        // init the route inputs
        initRouteInputs(context, route.getInputs());

        // abstracts is the cross cutting concerns
        List<ProcessorDefinition<?>> abstracts = new ArrayList<>();

        // upper is the cross cutting concerns such as interceptors, error handlers etc
        List<ProcessorDefinition<?>> upper = new ArrayList<>();

        // lower is the regular route
        List<ProcessorDefinition<?>> lower = new ArrayList<>();

        RouteDefinitionHelper.prepareRouteForInit(route, abstracts, lower);

        // parent and error handler builder should be initialized first
        initParentAndErrorHandlerBuilder(context, route, abstracts, onExceptions);
        // validate top-level violations
        validateTopLevel(route.getOutputs());
        // then interceptors
        initInterceptors(context, route, abstracts, upper, intercepts, interceptFromDefinitions, interceptSendToEndpointDefinitions);
        // then on completion
        initOnCompletions(abstracts, upper, onCompletions);
        // then sagas
        initSagas(abstracts, lower);
        // then transactions
        initTransacted(abstracts, lower);
        // then on exception
        initOnExceptions(abstracts, upper, onExceptions);

        // rebuild route as upper + lower
        route.clearOutput();
        route.getOutputs().addAll(lower);
        route.getOutputs().addAll(0, upper);
    }
}