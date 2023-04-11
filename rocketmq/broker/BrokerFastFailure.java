class BrokerController{

	public BrokerController(
        final BrokerConfig brokerConfig,
        final NettyServerConfig nettyServerConfig,
        final NettyClientConfig nettyClientConfig,
        final MessageStoreConfig messageStoreConfig
    ){
		..
		this.brokerFastFailure = new BrokerFastFailure(this);
		..
    }

    public void start(){
    	..
    	this.brokerFastFailure.start();
    	..
    }
}




class BrokerFastFailure{

	public void start() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (brokerController.getBrokerConfig().isBrokerFastFailureEnable()) {
                    cleanExpiredRequest();
                }
            }
        }, 1000, 10, TimeUnit.MILLISECONDS);
    }

    private void cleanExpiredRequest() {
    	while (this.brokerController.getMessageStore().isOSPageCacheBusy()) {
    		..
    		if (!this.brokerController.getSendThreadPoolQueue().isEmpty()) {
                final Runnable runnable = this.brokerController.getSendThreadPoolQueue().poll(0, TimeUnit.SECONDS);
                if (null == runnable) {
                    break;
                }

                final RequestTask rt = castRunnable(runnable);
                rt.returnResponse(RemotingSysResponseCode.SYSTEM_BUSY, String.format("[PCBUSY_CLEAN_QUEUE]broker busy, start flow control for a while, period in queue: %sms, size of queue: %d", System.currentTimeMillis() - rt.getCreateTimestamp(), this.brokerController.getSendThreadPoolQueue().size()));
            } else {
                break;
            }
            ..
    	}
    }




}