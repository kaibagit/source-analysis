// 索引创建核心链路
// 在消息写入MappedFile的byteBuffer后
// 会一直有个ReputMessageService线程，轮询查询MappedFile，并根据未处理了的消息生成index
ReputMessageService.run()
    => ReputMessageService.doReput()
        => DefaultMessageStore.doDispatch(dispatchRequest)
            => CommitLogDispatcherBuildIndex.dispatch(request)
                => IndexService.buildIndex(req)
                    => IndexService.putKey(indexFile, msg, idxKey)
                        => IndexFile.putKey(key, phyOffset, storeTimestamp)



// DefaultMessageStore内部类
class ReputMessageService extends ServiceThread {

    private volatile long reputFromOffset = 0;

    public void run() {
        DefaultMessageStore.log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                Thread.sleep(1);
                this.doReput();
            } catch (Exception e) {
                DefaultMessageStore.log.warn(this.getServiceName() + " service has exception. ", e);
            }
        }

        DefaultMessageStore.log.info(this.getServiceName() + " service end");
    }

    private void doReput() {
        for (boolean doNext = true; this.isCommitLogAvailable() && doNext; ) {
            //找到上次处理的MappedFile所对用的ByteBuffer
            SelectMappedBufferResult result = DefaultMessageStore.this.commitLog.getData(reputFromOffset);
            if (result != null) {
                this.reputFromOffset = result.getStartOffset();
                for (int readSize = 0; readSize < result.getSize() && doNext; ) {
                    // 开始从ByteMapper中读取消息，根据一个消息构建DispatchRequest，并返回
                    DispatchRequest dispatchRequest =
                                DefaultMessageStore.this.commitLog.checkMessageAndReturnSize(result.getByteBuffer(), false, false);
                    ..
                    DefaultMessageStore.this.doDispatch(dispatchRequest);
                    ..
                    this.reputFromOffset += size;   //size为本次消息体的大小
                    readSize += size;
                    ..
                }
            }else{
                doNext = false;
            }
        }
    }
}




class DefaultMessageStore {
    private final LinkedList<CommitLogDispatcher> dispatcherList;

    public DefaultMessageStore(final MessageStoreConfig messageStoreConfig, final BrokerStatsManager brokerStatsManager,
        final MessageArrivingListener messageArrivingListener, final BrokerConfig brokerConfig){
        ..
        this.dispatcherList = new LinkedList<>();
        this.dispatcherList.addLast(new CommitLogDispatcherBuildConsumeQueue());
        this.dispatcherList.addLast(new CommitLogDispatcherBuildIndex());
        ..
    }
}

// DefaultMessageStore内部类
class CommitLogDispatcherBuildIndex implements CommitLogDispatcher {

    @Override
    public void dispatch(DispatchRequest request) {
        if (DefaultMessageStore.this.messageStoreConfig.isMessageIndexEnable()) {
            DefaultMessageStore.this.indexService.buildIndex(request);
        }
    }
}


class IndexService{

    private final int indexNum; //索引文件数量，默认为2千万

    public void buildIndex(DispatchRequest req) {
        ..
        if (req.getUniqKey() != null) {
            indexFile = putKey(indexFile, msg, buildKey(topic, req.getUniqKey()));  //索引的key为：{消息topic}#{消息key}
            ..
        }
    }
    private IndexFile putKey(IndexFile indexFile, DispatchRequest msg, String idxKey) {
        ..
        ok = indexFile.putKey(idxKey, msg.getCommitLogOffset(), msg.getStoreTimestamp());
        ..
    }


    public QueryOffsetResult queryOffset(String topic, String key, int maxNum, long begin, long end) {
        List<Long> phyOffsets = new ArrayList<Long>(maxNum);

        ..
        for (int i = this.indexFileList.size(); i > 0; i--) {
            IndexFile f = this.indexFileList.get(i - 1);
            ..
            if (f.isTimeMatched(begin, end)) {
                f.selectPhyOffset(phyOffsets, buildKey(topic, key), maxNum, begin, end, lastFile);
            }
            ..
        }
        ..

        return new QueryOffsetResult(phyOffsets, indexLastUpdateTimestamp, indexLastUpdatePhyoffset);
    }
    
}

class IndexFile{

    public boolean putKey(final String key, final long phyOffset, final long storeTimestamp) {
        ..
        int keyHash = indexKeyHashMethod(key);
        int slotPos = keyHash % this.hashSlotNum;
        int absSlotPos = IndexHeader.INDEX_HEADER_SIZE + slotPos * hashSlotSize;    //应该查询的slot在整个文件中的绝对位置
        ..
        int slotValue = this.mappedByteBuffer.getInt(absSlotPos);   //上一个slot冲突的索引，在整个文件的位置，0表示没有冲突
        ..
        // 索引内容在整个文件的绝对位置
        int absIndexPos =
                    IndexHeader.INDEX_HEADER_SIZE + this.hashSlotNum * hashSlotSize
                        + this.indexHeader.getIndexCount() * indexSize;
        // 往mappedByteBuffer中写入20 bytes的索引内容
        this.mappedByteBuffer.putInt(absIndexPos, keyHash);
        this.mappedByteBuffer.putLong(absIndexPos + 4, phyOffset);
        this.mappedByteBuffer.putInt(absIndexPos + 4 + 8, (int) timeDiff);
        this.mappedByteBuffer.putInt(absIndexPos + 4 + 8 + 4, slotValue);   //上一个slot冲突的索引是第几个写入索引，从而在slot冲突时，形成单向链表
        // 因为索引文件是顺序添加的，且大小固定，根据写入序号很容易计算出索引在整个文件中的位置

        this.mappedByteBuffer.putInt(absSlotPos, this.indexHeader.getIndexCount()); //在slot位置写入：当前写入的是第几个索引
        ..
    }

    public void selectPhyOffset(final List<Long> phyOffsets, final String key, final int maxNum,
        final long begin, final long end, boolean lock) {
        ..
        int keyHash = indexKeyHashMethod(key);
        int slotPos = keyHash % this.hashSlotNum;
        int absSlotPos = IndexHeader.INDEX_HEADER_SIZE + slotPos * hashSlotSize;
        ..
    }
}













