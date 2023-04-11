public abstract class QueueTransferTask {

    public void start() {
        RTopic<Long> schedulerTopic = getTopic();
        ...
        
        messageListenerId = schedulerTopic.addListener(new MessageListener<Long>() {
            @Override
            public void onMessage(String channel, Long startTime) {
                scheduleTask(startTime);
            }
        });
    }
    
}