DefaultMQPushConsumer{
	DefaultMQPushConsumerImpl{
		MQClientInstance{
			MQClientAPIImpl{
				NettyRemotingClient
			}
			PullMessageService
			RebalanceService
		}
	}
}

Conumser核心流程：
1.根据consumer_group生成重试队列的订阅信息
2.将关注的topic和重试队列topic，从NameServer拉取TopicRouteData（定时任务）
3.根据TopicRouteData，与brokers建立连接并心跳（定时任务）
4.将MessageQueue的ConsumerOffset上传到相关联的broker上（定时任务）




PullMessageService相关逻辑：
1.MQClientInstance创建时，会创建PullMessageService对象
2.MQClientInstance#start()时，会调用PullMessageService#start()，间接调用PullMessageService#run()



rebalance核心逻辑：
1、client定时发送心跳给所有broker，心跳中包含（clientId、consumerGroup，订阅的信息等）数据
2、RebalanceService每隔20s根据topic，从对应borker中拉取consumerGroup的clientId，进行排序，然后重新分配MessageQueue
3、当消费者变动时，broker会触发notifyConsumerIdsChanged(#)方法，发送通知给所有comsumer;consumer收到通知，也会触发自己的notifyConsumerIdsChanged(#)方法，然后消费端随机从一个broker中拉取消费者的cid列表，重新负载均衡。


pullMessage核心逻辑：
1、RebalanceService触发，创建PullRequest对象，并放入PullMessageService的pullRequestQueue
2、PullMessageService线程不断从pullRequestQueue中取出pullRequestQueue，提交给PullAPIWrapper，间接提交给MQClientAPIImpl异步处理
3、根据broker返回的结果，选择不同的策略，再把该PullRequest对象放入PullMessageService的pullRequestQueue，然后循环

consumeMessageService核心逻辑：
1、pullMessage拉取到msg列表之后，调用ConsumeMessageService.submitConsumeRequest(#)提交
2、调用listener.consumeMessage(#)消费消息，如果失败，则将消息重新send到broker的重试队列
3、更新offset