class DefaultMessageStore implements MessageStore{
	public DefaultMessageStore(final MessageStoreConfig messageStoreConfig, final BrokerStatsManager brokerStatsManager,
        final MessageArrivingListener messageArrivingListener, final BrokerConfig brokerConfig) throws IOException {
		
        ..
		this.scheduleMessageService = new ScheduleMessageService(this);
        ..
	}

	public boolean load() {
        boolean result = true;

        try {
        	// 检测上一次是不是正常关闭
            boolean lastExitOK = !this.isTempFileExist();

            if (null != scheduleMessageService) {
                result = result && this.scheduleMessageService.load();
            }

            // load Commit Log
            result = result && this.commitLog.load();

            // load Consume Queue
            result = result && this.loadConsumeQueue();

            if (result) {
                this.storeCheckpoint =
                    new StoreCheckpoint(StorePathConfigHelper.getStoreCheckpoint(this.messageStoreConfig.getStorePathRootDir()));

                this.indexService.load(lastExitOK);

                this.recover(lastExitOK);

                log.info("load over, and the max phy offset = {}", this.getMaxPhyOffset());
            }
        } catch (Exception e) {
            log.error("load exception", e);
            result = false;
        }

        if (!result) {
            this.allocateMappedFileService.shutdown();
        }

        return result;
    }


    public PutMessageResult putMessage(MessageExtBrokerInner msg) {
        ..
        PutMessageResult result = this.commitLog.putMessage(msg);
        ..
    }

    
}









