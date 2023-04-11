class DefaultMQProducer{

	protected final transient DefaultMQProducerImpl defaultMQProducerImpl;

	public DefaultMQProducer(final String producerGroup, RPCHook rpcHook) {
        this.producerGroup = producerGroup;
        defaultMQProducerImpl = new DefaultMQProducerImpl(this, rpcHook);
    }


    public void start() throws MQClientException {
        this.defaultMQProducerImpl.start();
    }

    public SendResult send(Message msg){
        return this.defaultMQProducerImpl.send(msg);
    }
}






















class DefaultMQProducerImpl{

	private MQClientInstance mQClientFactory;

    // key为发布过的topic
    private final ConcurrentMap<String/* topic */, TopicPublishInfo> topicPublishInfoTable =
        new ConcurrentHashMap<String, TopicPublishInfo>();

	public void start(final boolean startFactory{default:true}) throws MQClientException {
        switch (this.serviceState) {
            case CREATE_JUST:
                this.serviceState = ServiceState.START_FAILED;

                this.checkConfig();

                if (!this.defaultMQProducer.getProducerGroup().equals(MixAll.CLIENT_INNER_PRODUCER_GROUP)) {
                    this.defaultMQProducer.changeInstanceNameToPID();
                }

                this.mQClientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultMQProducer, rpcHook);

                boolean registerOK = mQClientFactory.registerProducer(this.defaultMQProducer.getProducerGroup(), this);
                。。。

                this.topicPublishInfoTable.put(this.defaultMQProducer.getCreateTopicKey(), new TopicPublishInfo());  //默认的CreateTopicKey为TBW102

                if (startFactory) {
                    mQClientFactory.start();
                }

                。。。
                this.serviceState = ServiceState.RUNNING;
                break;
            case RUNNING:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
                throw new MQClientException("The producer service state not OK, maybe started once, "
                    + this.serviceState
                    + FAQUrl.suggestTodo(FAQUrl.CLIENT_SERVICE_NOT_OK),
                    null);
            default:
                break;
        }

        this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
    }



    /**
    *   核心流程：
    *   1、查询topic的路由信息，如果不存在，则从NameServer拉取
    *   2、
    */
    public SendResult send(Message msg,long timeout) {
        return this.sendDefaultImpl(msg, CommunicationMode.SYNC, null, timeout);
    }
    private SendResult sendDefaultImpl(
        Message msg,
        final CommunicationMode communicationMode,
        final SendCallback sendCallback,
        final long timeout
    ) {
        。。。

        final long invokeID = random.nextLong();
        。。。

        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic()); //如果topic没有创建，会如何处理？
        。。。
        for (; times < timesTotal; times++) {   //timesTotal为总共尝试次数
            。。。
            MessageQueue mqSelected = this.selectOneMessageQueue(topicPublishInfo, lastBrokerName);
            。。。
            mq = mqSelected;
            。。。
            sendResult = this.sendKernelImpl(msg, mq, communicationMode, sendCallback, topicPublishInfo, timeout - costTime);
            。。。
            switch (communicationMode) {
                case ASYNC:
                    return null;
                case ONEWAY:
                    return null;
                case SYNC:
                    if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                        if (this.defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()) {
                            continue;
                        }
                    }

                    return sendResult;
                default:
                    break;
            }
            。。。
        }
    }

    // 发送message，自己指定要发送的MessageQueue
    private SendResult sendSelectImpl(
        Message msg,
        MessageQueueSelector selector,
        Object arg,
        final CommunicationMode communicationMode {default:CommunicationMode.SYNC},
        final SendCallback sendCallback {default:null}, final long timeout
    ){
        。。。
        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
        。。。
        MessageQueue mq = null;
        。。。
        mq = selector.select(topicPublishInfo.getMessageQueueList(), msg, arg);
        。。。
        return this.sendKernelImpl(msg, mq, communicationMode, sendCallback, null, timeout - costTime);
        。。。
    }


    private SendResult sendKernelImpl(final Message msg,
                                      final MessageQueue mq,
                                      final CommunicationMode communicationMode,
                                      final SendCallback sendCallback,
                                      final TopicPublishInfo topicPublishInfo,
                                      final long timeout) {
        。。。
        String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        。。。
        SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
        requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
        requestHeader.setTopic(msg.getTopic());
        requestHeader.setDefaultTopic(this.defaultMQProducer.getCreateTopicKey());
        requestHeader.setDefaultTopicQueueNums(this.defaultMQProducer.getDefaultTopicQueueNums());
        requestHeader.setQueueId(mq.getQueueId());
        requestHeader.setSysFlag(sysFlag);
        requestHeader.setBornTimestamp(System.currentTimeMillis());
        requestHeader.setFlag(msg.getFlag());
        requestHeader.setProperties(MessageDecoder.messageProperties2String(msg.getProperties()));
        requestHeader.setReconsumeTimes(0);
        requestHeader.setUnitMode(this.isUnitMode());
        requestHeader.setBatch(msg instanceof MessageBatch);
        if (requestHeader.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            。。。
        }
        SendResult sendResult = null;
        switch (communicationMode) {
            case ASYNC:
                。。。
                sendResult = this.mQClientFactory.getMQClientAPIImpl().sendMessage(
                    brokerAddr,
                    mq.getBrokerName(),
                    tmpMessage,
                    requestHeader,
                    timeout - costTimeAsync,
                    communicationMode,
                    sendCallback,
                    topicPublishInfo,
                    this.mQClientFactory,
                    this.defaultMQProducer.getRetryTimesWhenSendAsyncFailed(),
                    context,
                    this);
                break;
            case ONEWAY:
            case SYNC:
                。。。
                sendResult = this.mQClientFactory.getMQClientAPIImpl().sendMessage(
                    brokerAddr,
                    mq.getBrokerName(),
                    msg,
                    requestHeader,
                    timeout - costTimeSync,
                    communicationMode,
                    context,
                    this);
                break;
            default:
                assert false;
                break;
        }
        。。。
    }


    private TopicPublishInfo tryToFindTopicPublishInfo(final String topic) {
        TopicPublishInfo topicPublishInfo = this.topicPublishInfoTable.get(topic);
        // 如果TopicPublishInfo没有准备好，则从NameServer拉取
        if (null == topicPublishInfo || !topicPublishInfo.ok()) {
            this.topicPublishInfoTable.putIfAbsent(topic, new TopicPublishInfo());
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic);
            topicPublishInfo = this.topicPublishInfoTable.get(topic);
        }

        。。。
    }

}