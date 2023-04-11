// 异步写入消息核心流程
CommitLog.putMessage(msg)
	=> MappedFile.appendMessage(final MessageExtBrokerInner msg, final AppendMessageCallback cb)
		=> MappedFile.appendMessagesInner(final MessageExt messageExt, final AppendMessageCallback cb)
			=> MappedFile.appendMessagesInner(final MessageExt messageExt, final AppendMessageCallback cb)
				=> AppendMessageCallback(DefaultAppendMessageCallback).doAppend(final long fileFromOffset, final ByteBuffer byteBuffer,final int maxBlank, final MessageExtBrokerInner msg)
					=> ByteBuffer.put(byte[] src, int offset, int length)



// 异步刷盘核心流程
commitLogService(FlushRealTimeService).run()
	=> MappedFileQueue.flush(flushPhysicQueueLeastPages)
		=> MappedFile.flush(flushLeastPages)
			=> MappedByteBuffer.force()