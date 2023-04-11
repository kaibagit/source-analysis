class ClientManageProcessor{

	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
        throws RemotingCommandException {
        switch (request.getCode()) {
            case RequestCode.HEART_BEAT:
                return this.heartBeat(ctx, request);
            case RequestCode.UNREGISTER_CLIENT:
                return this.unregisterClient(ctx, request);
            case RequestCode.CHECK_CLIENT_CONFIG:
                return this.checkClientConfig(ctx, request);
            default:
                break;
        }
        return null;
    }

    public RemotingCommand heartBeat(ChannelHandlerContext ctx, RemotingCommand request) {
    	..
    	HeartbeatData heartbeatData = HeartbeatData.decode(request.getBody(), HeartbeatData.class);
    	ClientChannelInfo clientChannelInfo = new ClientChannelInfo(
            ctx.channel(),
            heartbeatData.getClientID(),
            request.getLanguage(),
            request.getVersion()
        );
        
    	heartbeatData.getConsumerDataSet().each |ConsumerData data|{
    		..
    		boolean changed = this.brokerController.getConsumerManager().registerConsumer(
                data.getGroupName(),
                clientChannelInfo,
                data.getConsumeType(),
                data.getMessageModel(),
                data.getConsumeFromWhere(),
                data.getSubscriptionDataSet(),
                isNotifyConsumerIdsChangedEnable
            );
            ..
    	}
    	heartbeatData.getProducerDataSet().each |ProducerData data|{
    		this.brokerController.getProducerManager().registerProducer(data.getGroupName(),
                clientChannelInfo);
    	}
    	..
    }

}