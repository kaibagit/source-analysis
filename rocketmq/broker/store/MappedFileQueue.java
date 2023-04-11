class MappedFileQueue{

	private final int mappedFileSize;	//默认为1G

	// 存储代表commit log文件的对象
	private final CopyOnWriteArrayList<MappedFile> mappedFiles = new CopyOnWriteArrayList<MappedFile>();

	// 从存储目录读取commit log文件列表，并创建MappedFile放入mappedFiles
	public boolean load() {
		..
	}

	public boolean flush(final int flushLeastPages) {	//flushLeastPages：flush最少的页数
		..
		MappedFile mappedFile = this.findMappedFileByOffset(this.flushedWhere, this.flushedWhere == 0);
        if (mappedFile != null) {
            long tmpTimeStamp = mappedFile.getStoreTimestamp();
            int offset = mappedFile.flush(flushLeastPages);		//返回当前MappedFile写入后的位置
            long where = mappedFile.getFileFromOffset() + offset;	//appedFile.fileFromOffset + offset 表示在整个MappedFile列表中的位置，从而可以定位到具体是哪个MappedFile的哪个offset（每个MappedFile文件大小相同）
            ..
            this.flushedWhere = where;
            ..
        }
        ..
	}
}






class MappedFile{

	private final AtomicInteger flushedPosition = new AtomicInteger(0);		//已经flush到磁盘的位置值

	protected ByteBuffer writeBuffer = null;	//只有开启了TransientStorePool才不为null

	// 对象初始化时调用
	private void init(final String fileName, final int fileSize){
		..
		this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
		this.mappedByteBuffer = this.fileChannel.map(MapMode.READ_WRITE, 0, fileSize);
		..
	}


	public AppendMessageResult appendMessagesInner(final MessageExt messageExt, final AppendMessageCallback cb) {
		..
		int currentPos = this.wrotePosition.get();
		..
		ByteBuffer byteBuffer = writeBuffer != null ? writeBuffer.slice() : this.mappedByteBuffer.slice();	//只有开启了TransientStorePool才不为null
		// mappedByteBuffer的position始终为0，通过wrotePosition来定位（猜测为了统一writeBuffer和mappedByteBuffer的position）
        byteBuffer.position(currentPos);
        ..
		result = cb.doAppend(this.getFileFromOffset(), byteBuffer, this.fileSize - currentPos, (MessageExtBrokerInner) messageExt);
		..
	}

	public int flush(final int flushLeastPages) {
		..
		int value = getReadPosition();
		..
		this.mappedByteBuffer.force();	//强制刷盘
		..
		this.flushedPosition.set(value);
        this.release();
        ..
        return this.getFlushedPosition();
	}
}




// CommitLog内部类
class DefaultAppendMessageCallback implements AppendMessageCallback {

	// Store the message content
    private final ByteBuffer msgStoreItemMemory;
    // The maximum length of the message
    private final int maxMessageSize;

	DefaultAppendMessageCallback(final int size) {	//size默认配置4M
        this.msgIdMemory = ByteBuffer.allocate(MessageDecoder.MSG_ID_LENGTH);	//默认16字节
        this.msgStoreItemMemory = ByteBuffer.allocate(size + END_FILE_MIN_BLANK_LENGTH);	//END_FILE_MIN_BLANK_LENGTH=8
        this.maxMessageSize = size;
    }

	public AppendMessageResult doAppend(final long fileFromOffset, final ByteBuffer byteBuffer, final int maxBlank,
            final MessageExtBrokerInner msgInner) {	//maxBlank：文件剩余空闲大小
		..
		// Initialization of storage space
		this.resetByteBuffer(msgStoreItemMemory, msgLen);
		// 序列化写入this.msgStoreItemMemory
		..
		byteBuffer.put(this.msgStoreItemMemory.array(), 0, msgLen);
	}
}














