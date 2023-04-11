DefaultCamelContext#addRoutes(builder)
	-> RouteBuilder#addRoutesToCamelContext(context)
		-> RouteBuilder#populateRoutes()
			-> DefaultCamelContext#addRouteDefinitions(routeDefinitions)
				-> DefaultCamelContext#startRouteDefinitions(list)
					-> DefaultCamelContext#startRoute(route)
						-> RouteDefinition#prepare()
						-> RouteDefinition#addRoutes(context,routes)
						-> DefaultCamelContext#startRouteService(routeService,addingRoutes)

RouteBuilder#from()
	-> RoutesDefinition#from()
		-> RouteDefinition#from()
		-> RoutesDefinition#route(route)


// 查找Component
DefaultCamelContext#startRoute(RouteDefinition)
	-> RouteDefinition#addRoutes(ModelCamelContext,Collection<Route>)
		-> RouteDefinition#addRoutes(CamelContext,Collection<Route>,FromDefinition)
			-> DefaultRouteContext#getEndpoint()
				-> FromDefinition#resolveEndpoint(RouteContext)
					-> ..
						-> DefaultCamelContext#getEndpoint(String)
							-> DefaultCamelContext#getComponent(String)





// 组件间层次关系
RoutesDefinition{
	RouteDefinition{
		FromDefinition[]
		ProcessorDefinition[]
	}[]
}

DefaultCamelContext{
	RouteDefinition{
		FromDefinition[]
		ProcessorDefinition[]
	}[]
	RouteService[]
}

RouteService{
	RouteDefinition
}