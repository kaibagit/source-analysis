class DruidDataSource{

    protected void recycle(DruidPooledConnection pooledConnection){
        ..
        if ((!isAutoCommit) && (!isReadOnly)) {
            pooledConnection.rollback();
        }
        ..
        activeCount--;
        result = putLast(holder, currentTimeMillis);
        ..
    }

    boolean putLast(DruidConnectionHolder e, long lastActiveTimeMillis) {
        ..
        e.lastActiveTimeMillis = lastActiveTimeMillis;
        connections[poolingCount] = e;
        incrementPoolingCount();

        ..

        notEmpty.signal();
        ..

        return true;
    }

    // 用于CreateConnectionThread和CreateConnectionTask调用
    protected boolean put(PhysicalConnectionInfo physicalConnectionInfo) {
        DruidConnectionHolder holder = new DruidConnectionHolder(DruidDataSource.this, physicalConnectionInfo);
        ..

        return put(holder);
    }
    // keeyAlive校验通过，也会调用该方法返还
    private boolean put(DruidConnectionHolder holder) {
        ..
        connections[poolingCount] = holder;
        incrementPoolingCount();
        ..
        notEmpty.signal();
        ..
    }
}