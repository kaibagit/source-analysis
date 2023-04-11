基于版本：2.7

class Log4jLogger{
	public void warn(final String format, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.WARN, null, format, t);
    }
}


interface Logger{

}

// 扩展Logger接口和方法，用于方便实现和扩展
interface ExtendedLogger extends Logger{

}

class AbstractLogger implements ExtendedLogger{

	public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(fqcn, level, marker, message, t);
        }
    }
    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable t) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), t);
    }

    // 记录指定级别的日志，调用者需要确保该级别是enable的。
    void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t);
}

// The core implementation of the org.apache.logging.log4j.Logger interface.
class Logger extends AbstractLogger{

	@Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message,
            final Throwable t) {
        final Message msg = message == null ? new SimpleMessage(Strings.EMPTY) : message;
        final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
        strategy.log(this, getName(), fqcn, marker, level, msg, t);
    }
    
}