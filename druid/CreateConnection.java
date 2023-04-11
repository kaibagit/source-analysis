// DruidDataSource内部类
// 单线程创建Connection
class CreateConnectionThread{

	public void run() {
		。。。
		for (;;) {
			。。。
			if (emptyWait) {
                // 必须存在线程等待，才创建连接
                if (poolingCount >= notEmptyWaitThreadCount //
                        && !(keepAlive && activeCount + poolingCount < minIdle)) {
                    empty.await();
                }

                // 防止创建超过maxActive数量的连接
                if (activeCount + poolingCount >= maxActive) {
                    empty.await();
                    continue;
                }
            }
			。。。
			PhysicalConnectionInfo connection = null;
			。。。
			connection = createPhysicalConnection();	//调用DruidDataSource方法
			。。。
			boolean result = put(connection);	//调用DruidDataSource方法，将Connection放入池中
			。。。
		}
	}
	
}




// DruidDataSource内部类
// 线程池创建Connection
// 用于DruidDataSource的createScheduler使用
class CreateConnectionTask{

	// 创建一个新Connection
	private void runInternal() {
		for (;;) {
			..
			PhysicalConnectionInfo physicalConnection = createPhysicalConnection();
			..
			boolean result = put(physicalConnection);	//调用DruidDataSource方法，将Connection放入池中
			..
		}
	}

}