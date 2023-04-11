class DefaultMQPushConsumerImpl{

	public synchronized void start(){
		..
		if (this.getMessageListenerInner() instanceof MessageListenerOrderly) {
            this.consumeOrderly = true;
            this.consumeMessageService =
                new ConsumeMessageOrderlyService(this, (MessageListenerOrderly) this.getMessageListenerInner());
        } else if (this.getMessageListenerInner() instanceof MessageListenerConcurrently) {
            this.consumeOrderly = false;
            this.consumeMessageService =
                new ConsumeMessageConcurrentlyService(this, (MessageListenerConcurrently) this.getMessageListenerInner());
        }

        this.consumeMessageService.start();
		..
	}

    public void sendMessageBack(MessageExt msg, int delayLevel, final String brokerName){
        ..
        this.mQClientFactory.getMQClientAPIImpl().consumerSendMessageBack(brokerAddr, msg,
                this.defaultMQPushConsumer.getConsumerGroup(), delayLevel, 5000, getMaxReconsumeTimes());
        ..
    }
}











class MQClientAPIImpl{

    public void consumerSendMessageBack(
        final String addr,
        final MessageExt msg,
        final String consumerGroup,
        final int delayLevel,
        final long timeoutMillis,
        final int maxConsumeRetryTimes
    ){
        ConsumerSendMsgBackRequestHeader requestHeader = new ConsumerSendMsgBackRequestHeader();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CONSUMER_SEND_MSG_BACK, requestHeader);

        requestHeader.setGroup(consumerGroup);
        requestHeader.setOriginTopic(msg.getTopic());
        requestHeader.setOffset(msg.getCommitLogOffset());
        requestHeader.setDelayLevel(delayLevel);
        requestHeader.setOriginMsgId(msg.getMsgId());
        requestHeader.setMaxReconsumeTimes(maxConsumeRetryTimes);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
            request, timeoutMillis);
        ..
    }
}








class ConsumeMessageConcurrentlyService{

	private final ThreadPoolExecutor consumeExecutor;

	public void start() {
        //定时执行cleanExpireMsg()
    }

	public void submitConsumeRequest(
        final List<MessageExt> msgs,
        final ProcessQueue processQueue,
        final MessageQueue messageQueue,
        final boolean dispatchToConsume) {
		final int consumeBatchSize = this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize();	//默认值=1
		// 不多于批处理的数量，则封装成ConsumeRequest，直接提交给线程池处理
		if (msgs.size() <= consumeBatchSize) {
            ConsumeRequest consumeRequest = new ConsumeRequest(msgs, processQueue, messageQueue);
            try {
                this.consumeExecutor.submit(consumeRequest);
            } catch (RejectedExecutionException e) {
                this.submitConsumeRequestLater(consumeRequest);
            }
        // 如果多于批处理的数量，则将msgs按照consumeBatchSize数量拆分成多个consumeRequest，提交给线程池处理
        } else {
            for (int total = 0; total < msgs.size(); ) {
                List<MessageExt> msgThis = new ArrayList<MessageExt>(consumeBatchSize);
                for (int i = 0; i < consumeBatchSize; i++, total++) {
                    if (total < msgs.size()) {
                        msgThis.add(msgs.get(total));
                    } else {
                        break;
                    }
                }

                ConsumeRequest consumeRequest = new ConsumeRequest(msgThis, processQueue, messageQueue);
                try {
                    this.consumeExecutor.submit(consumeRequest);
                } catch (RejectedExecutionException e) {
                    for (; total < msgs.size(); total++) {
                        msgThis.add(msgs.get(total));
                    }

                    this.submitConsumeRequestLater(consumeRequest);
                }
            }
        }
	}

	public void processConsumeResult(
        final ConsumeConcurrentlyStatus status,
        final ConsumeConcurrentlyContext context,
        final ConsumeRequest consumeRequest
    ){
        // 默认值为Integer.MAX_VALUE
        // 当消费成功时，ackIndex为consumeRequest.getMsgs()的长度
        // 当消费失败时，ackIndex为-1
        int ackIndex = context.getAckIndex();
		..
		switch (status) {
            case CONSUME_SUCCESS:
                ..
            case RECONSUME_LATER:
                ..
            ..
		}

        switch (this.defaultMQPushConsumer.getMessageModel()) {
            case BROADCASTING:
                ..
            case CLUSTERING:
                ..
                // 如果消费失败，则将所有msg做如下处理：
                this.sendMessageBack(msg, context);
                ..
            ..
        }
        ..
        this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), offset, true);
		..
    }

    public boolean sendMessageBack(final MessageExt msg, final ConsumeConcurrentlyContext context) {
        int delayLevel = context.getDelayLevelWhenNextConsume();

        ..
        this.defaultMQPushConsumerImpl.sendMessageBack(msg, delayLevel, context.getMessageQueue().getBrokerName());
        return true;
        ..

        return false;
    }
}








// ConsumeMessageConcurrentlyService 内部类
class ConsumeRequest{

	public void run() {
		..
		ConsumeMessageConcurrentlyService.this.resetRetryTopic(msgs);	//如果msg的topic为%'RETRY%consumer_group'，则从属性中取出原topic，并设置到msg
		..
		status = listener.consumeMessage(Collections.unmodifiableList(msgs), context);
		..
		ConsumeMessageConcurrentlyService.this.processConsumeResult(status, context, this);
		..
	}

}





