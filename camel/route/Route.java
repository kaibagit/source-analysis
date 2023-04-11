基于版本：2.22.0

// A series of Camel routes
class RoutesDefinition{

	List<RouteDefinition> routes = new ArrayList<>();

	public RouteDefinition from(@AsEndpointUri String uri) {
        RouteDefinition route = createRoute();
        route.from(uri);
        return route(route);
    }

	// 创建一个新的RouteDefinition
    protected RouteDefinition createRoute() {
        RouteDefinition route = new RouteDefinition();
        ErrorHandlerFactory handler = getErrorHandlerBuilder();
        if (handler != null) {
            route.setErrorHandlerBuilderIfNull(handler);
        }
        return route;
    }

    // 将新的route纳入routes列表管理，并标记为prepared
    public RouteDefinition route(RouteDefinition route) {
        // must prepare the route before we can add it to the routes list
        RouteDefinitionHelper.prepareRoute(getCamelContext(), route, getOnExceptions(), getIntercepts(), getInterceptFroms(),
                getInterceptSendTos(), getOnCompletions());
        getRoutes().add(route);
        // mark this route as prepared
        route.markPrepared();
        return route;
    }

    public List<RouteContext> addRoutes(ModelCamelContext camelContext, Collection<Route> routes) throws Exception {
        ..
        for (FromDefinition fromType : inputs) {
            ..
            routeContext = addRoutes(camelContext, routes, fromType);
            ..
        }
        ..
    }

    protected RouteContext addRoutes(CamelContext camelContext, Collection<Route> routes, FromDefinition fromType) throws Exception {
        RouteContext routeContext = new DefaultRouteContext(camelContext, this, fromType, routes);
        ..
        routeContext.getEndpoint();
        ..
    }
}













// Represents the runtime objects for a given RouteDefinition so that it can be stopped independently of other routes
class RouteService{

    public RouteService(DefaultCamelContext camelContext, RouteDefinition routeDefinition, List<RouteContext> routeContexts, List<Route> routes) {
        this.camelContext = camelContext;
        this.routeDefinition = routeDefinition;
        this.routeContexts = routeContexts;
        this.routes = routes;
        this.id = routeDefinition.idOrCreate(camelContext.getNodeIdFactory());
    }

}

