interface EventExecutorGroup extends ScheduledExecutorService{

	EventExecutor next();

	Future<?> submit(Runnable task);

    <T> Future<T> submit(Runnable task, T result);

    <T> Future<T> submit(Callable<T> task);

    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
    
}