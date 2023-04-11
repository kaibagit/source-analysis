public class RedissonDelayedQueue<V> extends RedissonExpirable implements RDelayedQueue<V> {

    // name : 依赖RBlockingQueue的name
    protected RedissonDelayedQueue(QueueTransferService queueTransferService, Codec codec, final CommandAsyncExecutor commandExecutor, String name) {
        ...
        
        QueueTransferTask task = new QueueTransferTask(commandExecutor.getConnectionManager()) {
            ...
        };
        
        queueTransferService.schedule(getQueueName(), task);
        
        this.queueTransferService = queueTransferService;
    }

}