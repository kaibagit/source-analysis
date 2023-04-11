class DestroyConnectionThread{

	public void run(){
		..
		for (;;) {
			..
			if (timeBetweenEvictionRunsMillis > 0) {
                Thread.sleep(timeBetweenEvictionRunsMillis);
            } else {
                Thread.sleep(1000); //
            }
			..
			destroyTask.run();
			..
		}
		..
	}
}

class DestroyTask{

	public void run() {
        shrink(true, keepAlive);

        if (isRemoveAbandoned()) {
            removeAbandoned();
        }
    }
}

class DruidDataSource{
	public void shrink(boolean checkTime, boolean keepAlive) {
		..
		final int checkCount = poolingCount - minIdle;
        final long currentTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < poolingCount; ++i) {
        	DruidConnectionHolder connection = connections[i];

        	if (checkTime) {
        		// phyTimeoutMillis默认为-1，如果要限制Connection最大存活时间，则改为正数
                if (phyTimeoutMillis > 0) {
                    long phyConnectTimeMillis = currentTimeMillis - connection.connectTimeMillis;
                    if (phyConnectTimeMillis > phyTimeoutMillis) {
                        evictConnections[evictCount++] = connection;
                        continue;
                    }
                }

                // Connection闲置时间
                long idleMillis = currentTimeMillis - connection.lastActiveTimeMillis;

                // connections[]中Connection处理策略：
                // 如果闲置时间没有超过，则跳过
                // 0~(poolingCount - minIdle)，都是需要回收的
                // 剩余的Connection，即minIdle数量的Connection，如果闲置时间超出，也进行回收
                // 其他的，如果需要keepAlive，则全部放入keepAliveConnections[]中，进行保活处理

                if (idleMillis < minEvictableIdleTimeMillis) {
                    break;
                }

                if (checkTime && i < checkCount) {
                    evictConnections[evictCount++] = connection;
                } else if (idleMillis > maxEvictableIdleTimeMillis) {
                    evictConnections[evictCount++] = connection;
                } else if (keepAlive) {
                    keepAliveConnections[keepAliveCount++] = connection;
                }
            } else {
                ..
            }
        }
        int removeCount = evictCount + keepAliveCount;
        ..
        // 从connections中移除掉removeCount数量的Connection
        ..
        // 将evictConnections中的Connection一次调用JdbcUtils.close(connection)
        ..
        // 遍历keepAliveConnections，分别执行validateConnection(connection)，如果成功，则将Conection返回到连接池中
        ..
	}
}



