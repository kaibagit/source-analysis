class DefaultCamelContext extends ServiceSupport implements ModelCamelContext, Suspendable {

	List<RouteDefinition> routeDefinitions = new ArrayList<>();



    // start入口
	public void start() {
		..
		ServiceHelper.startServices(this.routeController);
		..
		super.start();	//设置各种状态，调用doStart()
		..
		EventHelper.notifyCamelContextStarted(this);
		..
		for (StartupListener startup : startupListeners) {
            if (startup instanceof ExtendedStartupListener) {
                ((ExtendedStartupListener) startup).onCamelContextFullyStarted(this, isStarted());
            }
        }
    }
    protected synchronized void doStart() throws Exception {
        ..
        doStartCamel();
        ..
    }
    private void doStartCamel(){
    	// start management strategy before lifecycles are started
    	// start lifecycle strategies
    	// start notifiers as services

    	// must let some bootstrap service be started before we can notify the starting event
        EventHelper.notifyCamelContextStarting(this);

        // start components
        startServices(components.values());

        // start the route definitions before the routes is started
        startRouteDefinitions(routeDefinitions);

        // invoke this logic to warmup the routes and if possible also start the routes
        doStartOrResumeRoutes(routeServices, true, !doNotStartRoutesOnFirstStart, false, true);
    }




    // addRoutes入口
    public void addRoutes(final RoutesBuilder builder) throws Exception {
        doWithDefinedClassLoader(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                builder.addRoutesToCamelContext(DefaultCamelContext.this);
                return null;
            }
        });
    }
    // 将原有的routeDefinitions全部替换成新的routeDefinitions
    public synchronized void addRouteDefinitions(Collection<RouteDefinition> routeDefinitions) {
        ..
        for (RouteDefinition routeDefinition : routeDefinitions) {
            removeRouteDefinition(routeDefinition);
        }
        this.routeDefinitions.addAll(routeDefinitions);
        if (shouldStartRoutes()) {
            startRouteDefinitions(routeDefinitions);
        }
    }
    // 依次遍历启动RouteDefinition
    protected void startRouteDefinitions(Collection<RouteDefinition> list) throws Exception {
        if (list != null) {
            for (RouteDefinition route : list) {
                startRoute(route);
            }
        }
    }

    public void startRoute(RouteDefinition route) throws Exception {
        // assign ids to the routes and validate that the id's is all unique
        RouteDefinitionHelper.forceAssignIds(this, routeDefinitions);
        ..

        // indicate we are staring the route using this thread so
        // we are able to query this if needed
        isStartingRoutes.set(true);
        try {
            // must ensure route is prepared, before we can start it
            route.prepare(this);

            List<Route> routes = new ArrayList<>();
            List<RouteContext> routeContexts = route.addRoutes(this, routes);
            RouteService routeService = new RouteService(this, route, routeContexts, routes);
            startRouteService(routeService, true);
        } finally {
            // we are done staring routes
            isStartingRoutes.remove();
        }
    }

    protected synchronized void startRouteService(RouteService routeService, boolean addingRoutes) throws Exception {
        ..
        safelyStartRouteServices(true, true, true, false, addingRoutes, routeService);
        ..
    }

    private void doStartOrResumeRouteConsumers(Map<Integer, DefaultRouteStartupOrder> inputs, boolean resumeOnly, boolean addingRoute) throws Exception {
        ..
        for (Consumer consumer : routeService.getInputs().values()) {
            ..
            startService(consumer);
            ..
        }
        ..
    }

    private void startService(Service service) throws Exception {
        ..
        service.start();
    }




    public Endpoint getEndpoint(String uri) {
        ..
        Endpoint answer;
        ..
        Component component = getComponent(scheme);     //scheme通过解析uri得到，如jetty
        ..
        answer = component.createEndpoint(rawUri);
        ..
        return answer;
    }
}