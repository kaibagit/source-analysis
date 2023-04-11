class RebalanceService{

	private final MQClientInstance mqClientFactory;

	public void run() {
		..
		while (!this.isStopped()) {
            this.waitForRunning(waitInterval);	//默认等待20s
            this.mqClientFactory.doRebalance();
        }
        ..
	}
}










class MQClientInstance{

	public void doRebalance() {
		this.consumerTable.entrySet().each |Map.Entry<String, MQConsumerInner> entry|{
			MQConsumerInner impl = entry.getValue();
			..
			impl.doRebalance();
			..
		}
	}

	public List<String> findConsumerIdList(final String topic, final String group) {
		String brokerAddr = this.findBrokerAddrByTopic(topic);		//找到所有存在该topic的broker地址，随机选择一个地址
		..
		return this.mQClientAPIImpl.getConsumerIdListByGroup(brokerAddr, group, 3000);
		..
	}

}






class MQClientAPIImpl{

	public List<String> getConsumerIdListByGroup(
        final String addr,
        final String consumerGroup,
        final long timeoutMillis) {
		GetConsumerListByGroupRequestHeader requestHeader = new GetConsumerListByGroupRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_LIST_BY_GROUP, requestHeader);
		
		RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
            request, timeoutMillis);
		..
		GetConsumerListByGroupResponseBody body =
            GetConsumerListByGroupResponseBody.decode(response.getBody(), GetConsumerListByGroupResponseBody.class);
        return body.getConsumerIdList();
		..
	}
}












class DefaultMQPushConsumerImpl{

	private final RebalanceImpl rebalanceImpl = new RebalancePushImpl(this);

	public void doRebalance() {
        ..
        this.rebalanceImpl.doRebalance(this.isConsumeOrderly());
        ..
    }
}













class RebalancePushImpl extends RebalanceImpl{

	// 继承自RebalanceImpl，由RebalancePushImpl在start()时注入
	protected MQClientInstance mQClientFactory;

    // 继承自RebalanceImpl
    // 当updateTopicRouteInfoFromNameServer()时，会建立该信息
    protected final ConcurrentMap<String/* topic */, Set<MessageQueue>> topicSubscribeInfoTable =
        new ConcurrentHashMap<String, Set<MessageQueue>>();

	// 继承自RebalanceImpl
	// 订阅信息缓存，当调用DefaultMQPushConsumer#subscribe(topic,subExpression) 时，会往里面加入订阅信息
	protected final ConcurrentMap<String /* topic */, SubscriptionData> subscriptionInner =
        new ConcurrentHashMap<String, SubscriptionData>();

    public void doRebalance(final boolean isOrder) {
    	this.getSubscriptionInner().entrySet().each |Map.Entry<String, SubscriptionData> entry|{
    		final String topic = entry.getKey();
    		..
    		this.rebalanceByTopic(topic, isOrder);
    		..
    	}
    }

    // 继承自RebalanceImpl
    private void rebalanceByTopic(final String topic, final boolean isOrder) {
    	switch (messageModel) {
            case BROADCASTING: {
            	..
            }
            case CLUSTERING: {
            	Set<MessageQueue> mqSet = this.topicSubscribeInfoTable.get(topic); 
                List<String> cidAll = this.mQClientFactory.findConsumerIdList(topic, consumerGroup);    //这里的cid即为Cunsumer发送给broker心跳信息里的clientId
            	..
                List<MessageQueue> mqAll = new ArrayList<MessageQueue>();
                mqAll.addAll(mqSet);

                Collections.sort(mqAll);
                Collections.sort(cidAll);

                AllocateMessageQueueStrategy strategy = this.allocateMessageQueueStrategy;
                ..
                List<MessageQueue> allocateResult = strategy.allocate(
                            this.consumerGroup,
                            this.mQClientFactory.getClientId(),
                            mqAll,
                            cidAll);
                ..
                Set<MessageQueue> allocateResultSet = new HashSet<MessageQueue>().addAll(allocateResult);
                ..
                boolean changed = this.updateProcessQueueTableInRebalance(topic, allocateResultSet, isOrder);
                ..
            }
    	..
    }
    // 继承自RebalanceImpl
    private boolean updateProcessQueueTableInRebalance(final String topic, final Set<MessageQueue> mqSet,
        final boolean isOrder) {
        ..
        List<PullRequest> pullRequestList = new ArrayList<PullRequest>();
        mqSet.each |MessageQueue mq|{
            ..
            PullRequest pullRequest = new PullRequest();
            pullRequest.setConsumerGroup(consumerGroup);
            pullRequest.setNextOffset(nextOffset);
            pullRequest.setMessageQueue(mq);
            pullRequest.setProcessQueue(pq);
            pullRequestList.add(pullRequest);
            ..
        }

        this.dispatchPullRequest(pullRequestList);
        ..
    }

    public void dispatchPullRequest(List<PullRequest> pullRequestList) {
        pullRequestList.each |PullRequest pullRequest|{
            this.defaultMQPushConsumerImpl.executePullRequestImmediately(pullRequest);
            ..
        }
    }
}













