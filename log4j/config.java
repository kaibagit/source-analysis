class LoggerConfig{
	final ReliabilityStrategy reliabilityStrategy;

	// whether location should be passed downstream
	// true if logging is synchronous or false if logging is asynchronous.
	boolean includeLocation = true;

	AppenderControlArraySet appenders = new AppenderControlArraySet();

	public LoggerConfig() {
        this.logEventFactory = LOG_EVENT_FACTORY;
        this.level = Level.ERROR;
        this.name = Strings.EMPTY;
        this.properties = null;
        this.propertiesRequireLookup = false;
        this.config = null;
        this.reliabilityStrategy = new DefaultReliabilityStrategy(this);
    }

    public void log(final String loggerName, final String fqcn, final Marker marker, final Level level,
            final Message data, final Throwable t) {
        List<Property> props = null;
        if (!propertiesRequireLookup) {
            props = properties;
        } else {
            if (properties != null) {
                props = new ArrayList<>(properties.size());
                final LogEvent event = Log4jLogEvent.newBuilder()
                        .setMessage(data)
                        .setMarker(marker)
                        .setLevel(level)
                        .setLoggerName(loggerName)
                        .setLoggerFqcn(fqcn)
                        .setThrown(t)
                        .build();
                for (int i = 0; i < properties.size(); i++) {
                    final Property prop = properties.get(i);
                    final String value = prop.isValueNeedsLookup() // since LOG4J2-1575
                            ? config.getStrSubstitutor().replace(event, prop.getValue()) //
                            : prop.getValue();
                    props.add(Property.createProperty(prop.getName(), value));
                }
            }
        }
        final LogEvent logEvent = logEventFactory.createEvent(loggerName, marker, fqcn, level, data, props, t);
        try {
            log(logEvent);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            ReusableLogEventFactory.release(logEvent);
        }
    }
    public void log(final LogEvent event) {
        if (!isFiltered(event)) {
            processLogEvent(event);
        }
    }

    private void processLogEvent(final LogEvent event) {
        event.setIncludeLocation(isIncludeLocation());
        callAppenders(event);	//遍历appenders，依次调用AppenderControl#callAppender(event)
        logParent(event);
    }
    protected void callAppenders(final LogEvent event) {
        final AppenderControl[] controls = appenders.get();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < controls.length; i++) {
            controls[i].callAppender(event);
        }
    }
}

//Asynchronous Logger object that is created via configuration and can be combined with synchronous loggers.
//AsyncLoggerConfig is a logger designed for high throughput and low latency logging. It does not perform any I/O in the calling (application) thread, but instead hands off the work to another thread as soon as possible. The actual logging is performed in the background thread. It uses the LMAX Disruptor library for inter-thread communication. (http://lmax-exchange.github.com/disruptor/)
//To use AsyncLoggerConfig, specify <asyncLogger> or <asyncRoot> in configuration.
//Note that for performance reasons, this logger does not include source location by default. You need to specify includeLocation="true" in the configuration or any %class, %location or %line conversion patterns in your log4j.xml configuration will produce either a "?" character or no output at all.
//For best performance, use AsyncLoggerConfig with the RandomAccessFileAppender or RollingRandomAccessFileAppender, with immediateFlush=false. These appenders have built-in support for the batching mechanism used by the Disruptor library, and they will flush to disk at the end of each batch. This means that even with immediateFlush=false, there will never be any items left in the buffer; all log events will all be written to disk in a very efficient manner.
class AsyncLoggerConfig extends LoggerConfig{

	final AsyncLoggerConfigDelegate delegate;

	@Override
    protected void callAppenders(final LogEvent event) {
        populateLazilyInitializedFields(event);

        if (!delegate.tryEnqueue(event, this)) {
            final EventRoute eventRoute = delegate.getEventRoute(event.getLevel());
            eventRoute.logMessage(this, event);
        }
    }

    void callAppendersInCurrentThread(final LogEvent event) {
        super.callAppenders(event);
    }
}