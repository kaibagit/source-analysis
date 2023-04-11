interface LogEvent{
	// Sets whether the source of the logging request is required downstream. 
	// Asynchronous Loggers and Appenders use this flag to determine whether to take a StackTrace snapshot or not before handing off this event to another thread.
	void setIncludeLocation(boolean locationRequired);
}

class Log4jLogEvent extends LogEvent{
	
	public ThrowableProxy getThrownProxy() {
        if (thrownProxy == null && thrown != null) {
            thrownProxy = new ThrowableProxy(thrown);
        }
        return thrownProxy;
    }
}


// Enumeration over the different destinations where a log event can be sent.
enum EventRoute{

	// Enqueues the event for asynchronous logging in the background thread.
	ENQUEUE{

	},

	// Logs the event synchronously: sends the event directly to the appender (in the current thread).
	SYNCHRONOUS{
		public void logMessage(final AsyncLoggerConfig asyncLoggerConfig, final LogEvent event) {
            asyncLoggerConfig.callAppendersInCurrentThread(event);
        }
	},

	DISCARD{

	}
}

interface AsyncQueueFullPolicy {

	EventRoute getRoute(final long backgroundThreadId, final Level level);

}