class FilterChainImpl{

	protected int                 pos = 0;

	private final DataSourceProxy dataSource;

    private final int             filterSize;

    public FilterChainImpl(DataSourceProxy dataSource){
        this.dataSource = dataSource;
        this.filterSize = getFilters().size();
    }

    // 从连接池中获取Connection
    public DruidPooledConnection dataSource_connect(DruidDataSource dataSource, long maxWaitMillis) throws SQLException {
        if (this.pos < filterSize) {
            DruidPooledConnection conn = nextFilter().dataSource_getConnection(this, dataSource, maxWaitMillis);
            return conn;
        }
        return dataSource.getConnectionDirect(maxWaitMillis);
    }

    // 创建实际Connection
    public ConnectionProxy connection_connect(Properties info) throws SQLException {
        if (this.pos < filterSize) {
            return nextFilter()
                    .connection_connect(this, info);
        }

        Driver driver = dataSource.getRawDriver();
        String url = dataSource.getRawJdbcUrl();

        Connection nativeConnection = driver.connect(url, info);

        if (nativeConnection == null) {
            return null;
        }

        return new ConnectionProxyImpl(dataSource, nativeConnection, info, dataSource.createConnectionId());
    }
}