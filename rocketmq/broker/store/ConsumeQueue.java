// ConsumeQueue创建核心链路
// 在消息写入MappedFile的byteBuffer后
// 会一直有个ReputMessageService线程，轮询查询MappedFile，并根据未处理了的消息生成ConsumeQueue
ReputMessageService.run()
    => ReputMessageService.doReput()
        => DefaultMessageStore.doDispatch(dispatchRequest)
            => CommitLogDispatcherBuildConsumeQueue.dispatch(request)
            	=> DefaultMessageStore.putMessagePositionInfo(request)

// DefaultMessageStore内部类
class CommitLogDispatcherBuildConsumeQueue{
	public void dispatch(DispatchRequest request) {
        final int tranType = MessageSysFlag.getTransactionValue(request.getSysFlag());
        switch (tranType) {
            case MessageSysFlag.TRANSACTION_NOT_TYPE:
            case MessageSysFlag.TRANSACTION_COMMIT_TYPE:
                DefaultMessageStore.this.putMessagePositionInfo(request);
                break;
            case MessageSysFlag.TRANSACTION_PREPARED_TYPE:
            case MessageSysFlag.TRANSACTION_ROLLBACK_TYPE:
                break;
        }
    }
}


class DefaultMessageStore{

	private final ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;

	public void putMessagePositionInfo(DispatchRequest dispatchRequest) {
		// 从consumeQueueTable中找出对应的ConsumeQueue，如果没有则创建
        ConsumeQueue cq = this.findConsumeQueue(dispatchRequest.getTopic(), dispatchRequest.getQueueId());
        cq.putMessagePositionInfoWrapper(dispatchRequest);
    }
}


// ConsumeQueue逻辑队列，可能对应一组文件
class ConsumeQueue{

	public void putMessagePositionInfoWrapper(DispatchRequest request) {
		..
		boolean result = this.putMessagePositionInfo(request.getCommitLogOffset(),
                request.getMsgSize(), tagsCode, request.getConsumeQueueOffset());
		..
	}

	private boolean putMessagePositionInfo(final long offset, final int size, final long tagsCode,
        final long cqOffset) {	// size：消息体大小
		..
		this.byteBufferIndex.flip();
        this.byteBufferIndex.limit(CQ_STORE_UNIT_SIZE);
        this.byteBufferIndex.putLong(offset);
        this.byteBufferIndex.putInt(size);
        this.byteBufferIndex.putLong(tagsCode);

        final long expectLogicOffset = cqOffset * CQ_STORE_UNIT_SIZE;

        // 根据目标写入的offset，找到对应的MappedFile
        MappedFile mappedFile = this.mappedFileQueue.getLastMappedFile(expectLogicOffset);
        ..
        return mappedFile.appendMessage(this.byteBufferIndex.array());
        ..
	}
}



// Extend of consume queue, to store something not important,such as message store time, filter bit map and etc.
class ConsumeQueueExt{

}


class MappedFileQueue{

}

// 对应一个磁盘上的文件
class MappedFile{

	// 往文件中追加数据
	public boolean appendMessage(final byte[] data) {
		int currentPos = this.wrotePosition.get();

        if ((currentPos + data.length) <= this.fileSize) {
            try {
                this.fileChannel.position(currentPos);
                this.fileChannel.write(ByteBuffer.wrap(data));
            } catch (Throwable e) {
                log.error("Error occurred when append message to mappedFile.", e);
            }
            this.wrotePosition.addAndGet(data.length);
            return true;
        }

        return false;
	}
}



