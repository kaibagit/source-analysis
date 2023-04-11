class CommitLog{

	private final MappedFileQueue mappedFileQueue;

	private final FlushCommitLogService commitLogService;	//刷盘服务

	public CommitLog(final DefaultMessageStore defaultMessageStore) {
		this.mappedFileQueue = new MappedFileQueue(defaultMessageStore.getMessageStoreConfig().getStorePathCommitLog(),
            defaultMessageStore.getMessageStoreConfig().getMapedFileSizeCommitLog(), defaultMessageStore.getAllocateMappedFileService());
		..
		// 根据不同刷盘策略，创建不同FlushCommitLogService，默认异步刷盘
		if (FlushDiskType.SYNC_FLUSH == defaultMessageStore.getMessageStoreConfig().getFlushDiskType()) {
            this.flushCommitLogService = new GroupCommitService();	//同步刷盘
        } else {
            this.flushCommitLogService = new FlushRealTimeService();	//异步刷盘
        }
		..
	}

	public void start() {
        this.flushCommitLogService.start();
        ..
    }

    public boolean load() {
        boolean result = this.mappedFileQueue.load();
        ..
    }

	public PutMessageResult putMessage(final MessageExtBrokerInner msg) {
		msg.setStoreTimestamp(System.currentTimeMillis());
		..
		MappedFile mappedFile = this.mappedFileQueue.getLastMappedFile();
		..
		result = mappedFile.appendMessage(msg, this.appendMessageCallback);
		..
		PutMessageResult putMessageResult = new PutMessageResult(PutMessageStatus.PUT_OK, result);
		..
		handleDiskFlush(result, putMessageResult, msg);
        handleHA(result, putMessageResult, msg);

        return putMessageResult;
	}
}

// CommitLog内部类，同步刷盘
class GroupCommitService extends FlushCommitLogService {

	public void run() {
		..
		while (!this.isStopped()) {
			..
            this.waitForRunning(10);
            this.doCommit();
            ..
        }
	}

	private void doCommit() {
		..
	}
}



// CommitLog内部类，异步刷盘
class FlushRealTimeService extends FlushCommitLogService {

	public void run() {
		..
		while (!this.isStopped()) {
			..
			int interval = CommitLog.this.defaultMessageStore.getMessageStoreConfig().getFlushIntervalCommitLog();	//默认500，CommitLog flush 到 磁盘的时间间隔
			int flushPhysicQueueLeastPages = CommitLog.this.defaultMessageStore.getMessageStoreConfig().getFlushCommitLogLeastPages();	//每次flush 最少的page数，默认4
			..
			this.waitForRunning(interval);
			..
			CommitLog.this.mappedFileQueue.flush(flushPhysicQueueLeastPages);
			..
		}
		..
	}

}