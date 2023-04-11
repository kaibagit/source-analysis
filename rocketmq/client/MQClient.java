class MQClientManager{

	private static MQClientManager instance = new MQClientManager();

	public MQClientInstance getAndCreateMQClientInstance(final ClientConfig clientConfig, RPCHook rpcHook) {
        String clientId = clientConfig.buildMQClientId();
        MQClientInstance instance = this.factoryTable.get(clientId);
        if (null == instance) {
            instance =
                new MQClientInstance(clientConfig.cloneClientConfig(),
                    this.factoryIndexGenerator.getAndIncrement(), clientId, rpcHook);
            MQClientInstance prev = this.factoryTable.putIfAbsent(clientId, instance);
            if (prev != null) {
                instance = prev;
                。。。
            } 
            。。。
        }

        return instance;
    }

}












class MQClientInstance{
    

    // key为producer_group，value为DefaultMQProducerImpl实例
    private final ConcurrentMap<String/* group */, MQProducerInner> producerTable = new ConcurrentHashMap();
    // key为consumer_group，value为DefaultMQPushConsumerImpl实例
	private final ConcurrentMap<String/* group */, MQConsumerInner> consumerTable = new ConcurrentHashMap();
    // 需要建立连接的brokers，通过关注的topic，然后updateTopicRouteInfoFromNameServer而来
    private final ConcurrentMap<String/* Broker Name */, HashMap<Long/* brokerId */, String/* address */>> brokerAddrTable =
        new ConcurrentHashMap<String, HashMap<Long, String>>();

    private final DefaultMQProducer defaultMQProducer;

    private final PullMessageService pullMessageService;



	public MQClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId, RPCHook rpcHook) {
		。。。
		this.clientRemotingProcessor = new ClientRemotingProcessor(this);
		this.mQClientAPIImpl = new MQClientAPIImpl(this.nettyClientConfig, this.clientRemotingProcessor, rpcHook, clientConfig);
        this.pullMessageService = new PullMessageService(this);
		。。。
        this.defaultMQProducer = new DefaultMQProducer(MixAll.CLIENT_INNER_PRODUCER_GROUP);
        this.defaultMQProducer.resetClientConfig(clientConfig);
        。。。
	}

    public void start() throws MQClientException {

        synchronized (this) {
            switch (this.serviceState) {
                case CREATE_JUST:
                    this.serviceState = ServiceState.START_FAILED;
                    // If not specified,looking address from name server
                    if (null == this.clientConfig.getNamesrvAddr()) {
                        this.mQClientAPIImpl.fetchNameServerAddr();
                    }
                    // Start request-response channel
                    this.mQClientAPIImpl.start();
                    // Start various schedule tasks
                    this.startScheduledTask();
                    // Start pull service
                    this.pullMessageService.start();
                    // Start rebalance service
                    this.rebalanceService.start();
                    // Start push service
                    this.defaultMQProducer.getDefaultMQProducerImpl().start(false);
                    log.info("the client factory [{}] start OK", this.clientId);
                    this.serviceState = ServiceState.RUNNING;
                    break;
                case RUNNING:
                    break;
                case SHUTDOWN_ALREADY:
                    break;
                case START_FAILED:
                    throw new MQClientException("The Factory object[" + this.getClientId() + "] has been created before, and failed.", null);
                default:
                    break;
            }
        }
    }

    private void startScheduledTask() {
        // 定时触发以下任务
        // mQClientAPIImpl.fetchNameServerAddr();   每2min
        // updateTopicRouteInfoFromNameServer();    默认每30s
        // cleanOfflineBroker();    默认每30s
        // sendHeartbeatToAllBrokerWithLock();  默认每30s
        // persistAllConsumerOffset();  默认每5s
        // adjustThreadPool();  每分钟
    }

    // 注册生产者
    public boolean registerProducer(final String group, final DefaultMQProducerImpl producer) {
        。。。

        MQProducerInner prev = this.producerTable.putIfAbsent(group, producer);
        。。。

        return true;
    }
    // 注册消费者
    public boolean registerConsumer(final String group, final MQConsumerInner consumer) {
        。。。

        MQConsumerInner prev = this.consumerTable.putIfAbsent(group, consumer);
        。。。

        return true;
    }

    

    // 定时任务触发，更新TopicRouteInfo
    public void updateTopicRouteInfoFromNameServer() {
        // 关注的topic
        Set<String> topicList = new HashSet<String>();

        // Consumer
        {
            this.consumerTable.entrySet().each |Entry<String, MQConsumerInner> entry|{
                MQConsumerInner impl = entry.getValue();
                if (impl != null) {
                    Set<SubscriptionData> subList = impl.subscriptions();   //间接调用rebalanceImpl.getSubscriptionInner().values()
                    if (subList != null) {
                        for (SubscriptionData subData : subList) {
                            topicList.add(subData.getTopic());
                        }
                    }
                }
            }
        }

        // Producer
        {
            // 初始时，会存在一个TBW102的topic
            this.producerTable.entrySet().each |Entry<String, MQProducerInner> entry| {
                MQProducerInner impl = entry.getValue();
                if (impl != null) {
                    Set<String> lst = impl.getPublishTopicList();
                    topicList.addAll(lst);
                }
            }
        }

        for (String topic : topicList) {
            this.updateTopicRouteInfoFromNameServer(topic);
        }
    }
    // 从NameServer更新TopicRouteInfo
    public boolean updateTopicRouteInfoFromNameServer(final String topic) {
        return updateTopicRouteInfoFromNameServer(topic, false, null);
    }
    public boolean updateTopicRouteInfoFromNameServer(final String topic, boolean isDefault,
        DefaultMQProducer defaultMQProducer) {
        ..
        topicRouteData = this.mQClientAPIImpl.getTopicRouteInfoFromNameServer(topic, 1000 * 3);
        ..
        TopicRouteData cloneTopicRouteData = topicRouteData.cloneTopicRouteData();

        topicRouteData.getBrokerDatas().each |BrokerData bd|{
            this.brokerAddrTable.put(bd.getBrokerName(), bd.getBrokerAddrs());
        }
        ..
        // Update sub info
        {
            Set<MessageQueue> subscribeInfo = topicRouteData2TopicSubscribeInfo(topic, topicRouteData);
            this.consumerTable.entrySet().each |Entry<String, MQConsumerInner> entry|{
                MQConsumerInner impl = entry.getValue();
                ..
                impl.updateTopicSubscribeInfo(topic, subscribeInfo);
            }
        }
        ..
        this.topicRouteTable.put(topic, cloneTopicRouteData);
        ..
    }
}


















// 封装了所有与remote server交互的api
class MQClientAPIImpl{

	public MQClientAPIImpl(final NettyClientConfig nettyClientConfig,
        final ClientRemotingProcessor clientRemotingProcessor,
        RPCHook rpcHook, final ClientConfig clientConfig) {
        。。。
        this.remotingClient = new NettyRemotingClient(nettyClientConfig, null);
        。。。
    }

	public void start() {
        this.remotingClient.start();
    }

    // 检测并更新NameServer地址
    public String fetchNameServerAddr() {
        。。。
    }

    public TopicRouteData getTopicRouteInfoFromNameServer(final String topic, final long timeoutMillis) {
        return getTopicRouteInfoFromNameServer(topic, timeoutMillis, true);
    }
    public TopicRouteData getTopicRouteInfoFromNameServer(final String topic, final long timeoutMillis,
        boolean allowTopicNotExist) {
        GetRouteInfoRequestHeader requestHeader = new GetRouteInfoRequestHeader();
        requestHeader.setTopic(topic);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ROUTEINTO_BY_TOPIC, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        。。。
        switch (response.getCode()) {
            case ResponseCode.TOPIC_NOT_EXIST: {
                if (allowTopicNotExist && !topic.equals(MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC)) {
                    log.warn("get Topic [{}] RouteInfoFromNameServer is not exist value", topic);
                }

                break;
            }
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return TopicRouteData.decode(body, TopicRouteData.class);
                }
            }
            default:
                break;
        }

        throw new MQClientException(response.getCode(), response.getRemark());
    }


    public SendResult sendMessage(
        final String addr,
        final String brokerName,
        final Message msg,
        final SendMessageRequestHeader requestHeader,
        final long timeoutMillis,
        final CommunicationMode communicationMode,
        final SendCallback sendCallback {default:null},
        final TopicPublishInfo topicPublishInfo {default:null},
        final MQClientInstance instance {default:null},
        final int retryTimesWhenSendFailed {default:0},
        final SendMessageContext context,
        final DefaultMQProducerImpl producer
    ) {
        。。。
        request = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, requestHeader);
        。。。
        switch (communicationMode) {
            case ONEWAY:
                this.remotingClient.invokeOneway(addr, request, timeoutMillis);
                return null;
            case ASYNC:
                final AtomicInteger times = new AtomicInteger();
                。。。
                this.sendMessageAsync(addr, brokerName, msg, timeoutMillis - costTimeAsync, request, sendCallback, topicPublishInfo, instance,
                    retryTimesWhenSendFailed, times, context, producer);
                return null;
            case SYNC:
                。。。
                return this.sendMessageSync(addr, brokerName, msg, timeoutMillis - costTimeSync, request);
            default:
                assert false;
                break;
        }

        return null;
    }
    private SendResult sendMessageSync(
        final String addr,
        final String brokerName,
        final Message msg,
        final long timeoutMillis,
        final RemotingCommand request
    ) {
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        。。。
        return this.processSendResponse(brokerName, msg, response);
    }

}