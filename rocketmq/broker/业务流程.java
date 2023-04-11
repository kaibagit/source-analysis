存储消息
SendMessageProcessor#processRequest（#）
-> SendMessageProcessor#sendMessage(#)
	-> DefaultMessageStore#putMessage(#)
		-> CommitLog#putMessage(#)