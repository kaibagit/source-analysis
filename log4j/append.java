// Data structure with similar semantics to CopyOnWriteArraySet, but giving direct access to the underlying array.
class AppenderControlArraySet{

}

class AppenderControl{

	public void callAppender(final LogEvent event) {
        if (shouldSkip(event)) {
            return;
        }
        callAppenderPreventRecursion(event);
    }
    // callAppender防止递归
    private void callAppenderPreventRecursion(final LogEvent event) {
        try {
            recursive.set(this);
            callAppender0(event);
        } finally {
            recursive.set(null);
        }
    }
    private void callAppender0(final LogEvent event) {
        ensureAppenderStarted();
        if (!isFilteredByAppender(event)) {
            tryCallAppender(event);
        }
    }
    private void tryCallAppender(final LogEvent event) {
        try {
            appender.append(event);
        } catch (final RuntimeException ex) {
            handleAppenderError(ex);
        } catch (final Exception ex) {
            handleAppenderError(new AppenderLoggingException(ex));
        }
    }

}


class RollingRandomAccessFileAppender extends AbstractOutputStreamAppender<RollingRandomAccessFileManager>{
	public void append(final LogEvent event) {
        final RollingRandomAccessFileManager manager = getManager();
        manager.checkRollover(event);

        // Leverage the nice batching behaviour of async Loggers/Appenders:
        // we can signal the file manager that it needs to flush the buffer
        // to disk at the end of a batch.
        // From a user's point of view, this means that all log events are
        // _always_ available in the log file, without incurring the overhead
        // of immediateFlush=true.
        manager.setEndOfBatch(event.isEndOfBatch()); // FIXME manager's EndOfBatch threadlocal can be deleted

        // LOG4J2-1292 utilize gc-free Layout.encode() method: taken care of in superclass
        super.append(event);
    }
    // AbstractOutputStreamAppender
    public void append(final LogEvent event) {
        try {
            tryAppend(event);
        } catch (final AppenderLoggingException ex) {
            error("Unable to write to stream " + manager.getName() + " for appender " + getName() + ": " + ex);
            throw ex;
        }
    }
    // AbstractOutputStreamAppender
    private void tryAppend(final LogEvent event) {
    	// ENABLE_DIRECT_ENCODERS：Kill switch for garbage-free Layout behaviour that encodes LogEvents directly into org.apache.logging.log4j.core.layout.ByteBufferDestinations without creating intermediate temporary Objects.
    	// 不创建中间对象而直接encode LogEvents 至 org.apache.logging.log4j.core.layout.ByteBufferDestinations，默认为true
        if (Constants.ENABLE_DIRECT_ENCODERS) {
            directEncodeEvent(event);
        } else {
            writeByteArrayToManager(event);
        }
    }
    protected void directEncodeEvent(final LogEvent event) {
        getLayout().encode(event, manager);
        if (this.immediateFlush || event.isEndOfBatch()) {
            manager.flush();
        }
    }
}