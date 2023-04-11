class DruidPooledConnection{

	protected Connection conn; //实际类型为ConnectionProxyImpl

	public DruidPooledConnection(DruidConnectionHolder holder){
        super(holder.getConnection());

        this.conn = holder.getConnection();
        this.holder = holder;
        dupCloseLogEnable = holder.getDataSource().isDupCloseLogEnable();
        ownerThread = Thread.currentThread();
        connectedTimeMillis = System.currentTimeMillis();
    }

    // Connection回收
    public void close() throws SQLException {
        。。。
        DruidConnectionHolder holder = this.holder;
        。。。
        DruidAbstractDataSource dataSource = holder.getDataSource();
        。。。
        List<Filter> filters = dataSource.getProxyFilters();
        if (filters.size() > 0) {
            FilterChainImpl filterChain = new FilterChainImpl(dataSource);
            filterChain.dataSource_recycle(this);
        } else {
            recycle();  //调用DruidDataSource方法
        }
        。。。
    }
    public void recycle() throws SQLException {
        。。。
        DruidConnectionHolder holder = this.holder;
        。。。
        DruidAbstractDataSource dataSource = holder.getDataSource();
        dataSource.recycle(this);
        。。。
    }


}

class DruidConnectionHolder{

	protected final Connection conn;	//jdbc原生Connection

	public DruidConnectionHolder(DruidAbstractDataSource dataSource, PhysicalConnectionInfo pyConnectInfo){
        this(dataSource,
            pyConnectInfo.getPhysicalConnection(),
            pyConnectInfo.getConnectNanoSpan(),
            pyConnectInfo.getVairiables(),
            pyConnectInfo.getGlobalVairiables());
    }

    public DruidConnectionHolder(DruidAbstractDataSource dataSource, Connection conn, long connectNanoSpan,
                                 Map<String, Object> variables, Map<String, Object> globleVariables){
    	。。。
    	this.conn = conn;
    	。。。
    }
}

class ConnectionProxyImpl{
    
}

class PhysicalConnectionInfo{

}


