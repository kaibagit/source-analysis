核心流程：
SendMessageProcessor.processRequest(ctx,request)
    => SendMessageProcessor.sendMessage(ctx,request,sendMessageContext,requestHeader)
        => MessageStore(DefaultMessageStore).putMessage(msg)
            => CommitLog.putMessage(msg)



// 用于处理producer publish的消息
class SendMessageProcessor{

	public RemotingCommand processRequest(ChannelHandlerContext ctx,
                                          RemotingCommand request){
		..
		response = this.sendMessage(ctx, request, mqtraceContext, requestHeader);
		..
	}

	private RemotingCommand sendMessage(final ChannelHandlerContext ctx,
                                        final RemotingCommand request,
                                        final SendMessageContext sendMessageContext,
                                        final SendMessageRequestHeader requestHeader){
		..
		putMessageResult = this.brokerController.getMessageStore().putMessage(msgInner);
        ..

        return handlePutMessageResult(putMessageResult, response, request, msgInner, responseHeader, sendMessageContext, ctx, queueIdInt);
	}
}