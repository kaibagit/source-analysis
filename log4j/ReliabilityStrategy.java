// 可靠性策略
// 确保log events能够投递到正确的appenders，即使系统运行过程中，配置被变更
class ReliabilityStrategy{

	// reconfigured: supplies the next LoggerConfig if the strategy's LoggerConfig is no longer active
	// loggerName: The name of the Logger.
	void log(Supplier<LoggerConfig> reconfigured, String loggerName, String fqcn, Marker marker, Level level,
            Message data, Throwable t);
}

// ReliabilityStrategy that counts the number of threads that have started to log an event but have not completed yet, 
// and waits for these threads to finish before allowing the appenders to be stopped.
class AwaitCompletionReliabilityStrategy{
	@Override
    public void log(final Supplier<LoggerConfig> reconfigured, final String loggerName, final String fqcn,
            final Marker marker, final Level level, final Message data, final Throwable t) {

        final LoggerConfig config = getActiveLoggerConfig(reconfigured);
        try {
            config.log(loggerName, fqcn, marker, level, data, t);
        } finally {
            config.getReliabilityStrategy().afterLogEvent();
        }
    }
}

// 在允许配置被停止之前，会无条件sleep一段时间
class AwaitUnconditionallyReliabilityStrategy{

}

// Reliability strategy that assumes reconfigurations will never take place.
class DefaultReliabilityStrategy{

}

// ReliabilityStrategy that uses read/write locks to prevent the LoggerConfig from stopping while it is in use.
class LockingReliabilityStrategy{

}