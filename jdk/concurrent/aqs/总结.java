AQS总共有2类队列：
1、SyncQueue同步队列：SyncQueue是一个双向队列，一个AQS维护一个SyncQueue，所有线程在这个队列中竞争锁
2、等待队列：每个Condition对象维护一个等待队列，等待队列是一个单向队列，当调用condition.await时，会加入该队列，signal只有，会从中移除，加入AQS的同步队列中