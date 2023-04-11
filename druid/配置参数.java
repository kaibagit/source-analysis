class DruidDataSource{
	int initialSize = 0;	//连接池创建时，初始化的连接数
	int minIdle = 0;	//当连接闲置时间达到minEvictableIdleTimeMillis，而（poolingCount-minIdle）数量的连接会被druid销毁
	int maxIdle = 8;	//为了兼容其他连接池的配置参数，配置了没有任何作用
	int maxActive = 8;	//池中最大连接数

	int poolingCount = 0;	//连接池中可用的Connection数量
	int activeCount = 0;	//活跃Connection数量，即已拿出的Connection数


	int createTaskCount = 0;	//正在创建Connection的线程数，如果采用CreateConnectionThread方式，该值===0
	int notEmptyWaitThreadCount = 0;	//连接池为空，等待获取连接的线程数量

	long maxWait = -1;	//应用从druid获取连接的超时时间。-1表示没有超时时间。


	/*
		createScheduler != null即用createScheduler来批量创建Connection，否则采用单线程方式创建
	*/
	ScheduledExecutorService createScheduler;
	/*
		允许并行创建Connection的线程数量;
		当createTaskCount >= maxCreateTaskCount时，则忽略emptySignal()
	*/
	int maxCreateTaskCount = 3;


	boolean testOnReturn = false;	// Connection返还给连接池时，是否执行test
	

	long timeBetweenEvictionRunsMillis = 60 * 1000L;	// 60秒，间隔一段时间执行shrink操作


	long phyTimeoutMillis = -1;	//物理连接的最长时间，当该值大于0时，如果超过该值，则关闭连接
	/*
		在Connection闲置>minEvictableIdleTimeMillis，且<maxEvictableIdleTimeMillis时，是否进行保活处理；
		druid通过mysql驱动提供的ping来保活的;
		如果keepAlive=false，如果建立的连接长时间不使用，会自动被销毁，导致poolingCount+activeCount<minIdle;而druid不会主动去检测来保持minIdle连接数量
	*/
	boolean keepAlive = false;


	DruidConnectionHolder[] evictConnections;	//即将销毁的Connection
	DruidConnectionHolder[]	keepAliveConnections;	//即将进行保活检查的Connection，如果成功，返回连接池中；失败则销毁
}