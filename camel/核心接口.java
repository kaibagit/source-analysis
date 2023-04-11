public interface CamelContext {

	void start() throws Exception;

	void stop() throws Exception;

	void addRoutes(RoutesBuilder builder) throws Exception;
}

// CamelContext的典型接口
public interface ModelCamelContext extends CamelContext {

	void addRouteDefinitions(Collection<RouteDefinition> routeDefinitions) throws Exception;

	void addRouteDefinition(RouteDefinition routeDefinition) throws Exception;
}



public interface RoutesBuilder {

    void addRoutesToCamelContext(CamelContext context) throws Exception;

}

public abstract class RouteBuilder extends BuilderSupport implements RoutesBuilder {

}

public class RouteDefinition {

}

// A Route  defines the processing used on an inbound message exchange from a specific Endpoint within a CamelContext.
public interface Route extends EndpointAware {
}



public interface Processor {

    void process(Exchange exchange) throws Exception;
}

// An Exchange is the message container holding the information during the entire routing of a Message received by a Consumer.
public interface Exchange {

	/**
     * Returns the inbound request message
     *
     * @return the message
     */
    Message getIn();

    Message getOut();
}

public interface Message {

	Object getBody();

	void setBody(Object body);
}