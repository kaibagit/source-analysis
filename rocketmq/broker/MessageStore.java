class DefaultMessageStore implements MessageStore{

	public DefaultMessageStore(final MessageStoreConfig messageStoreConfig, final BrokerStatsManager brokerStatsManager,
        final MessageArrivingListener messageArrivingListener, final BrokerConfig brokerConfig) throws IOException {
		..
        this.commitLog = new CommitLog(this);
        ..
		this.scheduleMessageService = new ScheduleMessageService(this);
        ..
	}

    public void start() throws Exception {
        ..
        this.commitLog.start();
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
        if (this.isOSPageCacheBusy()) {
            return new PutMessageResult(PutMessageStatus.OS_PAGECACHE_BUSY, null);
        }
        ..
        PutMessageResult result = this.commitLog.putMessage(msg);
        ..
    }

    public boolean isOSPageCacheBusy() {
        long begin = this.getCommitLog().getBeginTimeInLock();  //当有msg正在写入时，beginTimeInLock为开始写入时间；否则为0
        long diff = this.systemClock.now() - begin;

        // osPageCacheBusyTimeOutMills默认值为1000，当写入超过1000ms时，则认为busy
        return diff < 10000000
                && diff > this.messageStoreConfig.getOsPageCacheBusyTimeOutMills();     
    }


}