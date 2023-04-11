class ConsumerManageProcessor{

	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) {
        switch (request.getCode()) {
            case RequestCode.GET_CONSUMER_LIST_BY_GROUP:
                return this.getConsumerListByGroup(ctx, request);
            case RequestCode.UPDATE_CONSUMER_OFFSET:
                return this.updateConsumerOffset(ctx, request);
            case RequestCode.QUERY_CONSUMER_OFFSET:
                return this.queryConsumerOffset(ctx, request);
            default:
                break;
        }
        return null;
    }

    public RemotingCommand getConsumerListByGroup(ChannelHandlerContext ctx, RemotingCommand request){
    	..
    	ConsumerGroupInfo consumerGroupInfo =
            this.brokerController.getConsumerManager().getConsumerGroupInfo(
                requestHeader.getConsumerGroup());
        ..
        List<String> clientIds = consumerGroupInfo.getAllClientId();
        ..
        GetConsumerListByGroupResponseBody body = new GetConsumerListByGroupResponseBody();
            body.setConsumerIdList(clientIds);
            response.setBody(body.encode());
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);
            return response;
        ..
    }
}