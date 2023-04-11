class MQClientInstance{

	// 发送心跳给brokers
    public void sendHeartbeatToAllBrokerWithLock() {
        ..
        this.sendHeartbeatToAllBroker();
        this.uploadFilterClassSource();
        ..
    }
    private void sendHeartbeatToAllBroker() {
        ..
        final HeartbeatData heartbeatData = this.prepareHeartbeatData();
        ..
        this.brokerAddrTable.each |Entry<String, HashMap<Long, String>> entry|{
            entry.getValue().each |Map.Entry<Long, String> entry1| {
                ..
                String addr = entry1.getValue();
                ..
                int version = this.mQClientAPIImpl.sendHearbeat(addr, heartbeatData, 3000);
                ..
            }
        }
    }


    private HeartbeatData prepareHeartbeatData() {
    	HeartbeatData heartbeatData = new HeartbeatData();

    	// clientID
        heartbeatData.setClientID(this.clientId);

        // Consumer
        consumerTable.entrySet().each |Map.Entry<String, MQConsumerInner> entry| {
        	Map.Entry<String, MQConsumerInner> entry
        	..
        	ConsumerData consumerData = new ConsumerData();
            consumerData.setGroupName(impl.groupName());
            consumerData.setConsumeType(impl.consumeType());
            consumerData.setMessageModel(impl.messageModel());
            consumerData.setConsumeFromWhere(impl.consumeFromWhere());
            consumerData.getSubscriptionDataSet().addAll(impl.subscriptions());
            consumerData.setUnitMode(impl.isUnitMode());

            heartbeatData.getConsumerDataSet().add(consumerData);
        }

        // Producer
        producerTable.entrySet().each |Map.Entry<String/* group */, MQProducerInner> entry|{
        	MQProducerInner impl = entry.getValue();
        	..
        	ProducerData producerData = new ProducerData();
            producerData.setGroupName(entry.getKey());

            heartbeatData.getProducerDataSet().add(producerData);
        }

        return heartbeatData;
    }

}





class MQClientAPIImpl{

	public int sendHearbeat(
        final String addr,
        final HeartbeatData heartbeatData,
        final long timeoutMillis
    ) {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
        request.setLanguage(clientConfig.getLanguage());
        request.setBody(heartbeatData.encode());
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        ..
    }

}