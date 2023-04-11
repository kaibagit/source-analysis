class DruidDataSource{

	protected List<Filter> filters = new CopyOnWriteArrayList<Filter>();

	public void setProxyFilters(List<Filter> filters) {
        if (filters != null) {
            this.filters.addAll(filters);
        }
    }


    private int poolingCount = 0;	//池中的Connection数
    protected volatile int initialSize = DEFAULT_INITIAL_SIZE;
    protected volatile int maxActive = DEFAULT_MAX_ACTIVE_SIZE;	//最大活跃数，活跃数=activeCount + poolingCount
    private volatile DruidConnectionHolder[] connections;	//连接池中的连接，从池中取出时，需封装成DruidPooledConnection

    // 继承自DruidAbstractDataSource
    protected ScheduledExecutorService createScheduler;	//创建Connection的线程池
    // 继承自DruidAbstractDataSource
    protected Condition notEmpty;	//notEmpty的Condition，用于触发CreateConnection创建Connection
    protected Condition empty;		//empty的Condition，用于触发阻塞线程从池中拿Connection

    public init(){
    	..
        // 初始化创建initialSize数量的Connection
    	..
    	createAndStartCreatorThread();	//创建CreateConnectionThread并start()
        createAndStartDestroyThread();  //创建DestroyConnectionThread并start()
    	..
    }


	public DruidPooledConnection getConnection(long maxWaitMillis{default:-1}) throws SQLException {
        init();

        if (filters.size() > 0) {
            FilterChainImpl filterChain = new FilterChainImpl(this);
            return filterChain.dataSource_connect(this, maxWaitMillis);
        } else {
            return getConnectionDirect(maxWaitMillis);
        }
    }


    public DruidPooledConnection getConnectionDirect(long maxWaitMillis) throws SQLException {
    	。。。
    	DruidPooledConnection poolableConnection;
    	。。。
    	poolableConnection = getConnectionInternal(maxWaitMillis);
    	。。。
    }

    private DruidPooledConnection getConnectionInternal(long maxWait){
    	。。。
    	DruidConnectionHolder holder;
    	。。。
    	lock.lockInterruptibly();	//获取锁
    	。。。
    	if (maxWait > 0) {
            holder = pollLast(nanos);
        } else {
            holder = takeLast();
        }
    	。。。
    	lock.unlock();
    	。。。
    	DruidPooledConnection poolalbeConnection = new DruidPooledConnection(holder);
        return poolalbeConnection;
    }

    DruidConnectionHolder takeLast(){
    	。。。
    	while (poolingCount == 0) {
            emptySignal(); // send signal to CreateThread create connection

            。。。

            notEmpty.await(); // signal by recycle or creator

            。。。
        }
    	。。。
    	DruidConnectionHolder last = connections[poolingCount];
        connections[poolingCount] = null;

        return last;
    }

    public Connection createPhysicalConnection(String url, Properties info) throws SQLException {
        Connection conn;
        if (getProxyFilters().size() == 0) {
            conn = getDriver().connect(url, info);
        } else {
            conn = new FilterChainImpl(this).connection_connect(info);
        }

        createCountUpdater.incrementAndGet(this);

        return conn;
    }

    // 继承自DruidAbstractDataSource
    public PhysicalConnectionInfo createPhysicalConnection() throws SQLException {
    	。。。
    	conn = createPhysicalConnection(url, physicalConnectProperties);
    	。。。
    	return new PhysicalConnectionInfo(conn, connectStartNanos, connectedNanos, initedNanos, validatedNanos, variables, globalVariables);
    }


    protected void createAndStartCreatorThread() {
        if (createScheduler == null) {
            。。。
            createConnectionThread = new CreateConnectionThread(threadName);
            createConnectionThread.start();
            return;
        }
        。。。
    }

    protected boolean put(PhysicalConnectionInfo physicalConnectionInfo) {
    	DruidConnectionHolder holder = null;
    	。。。
    	holder = new DruidConnectionHolder(DruidDataSource.this, physicalConnectionInfo);
    	。。。
    	return put(holder);
    }
    private boolean put(DruidConnectionHolder holder) {
    	。。。
    	connections[poolingCount] = holder;
    	incrementPoolingCount();
    	。。。
    	notEmpty.signal();
    	。。。
    }

    // 如果使用createScheduler方式，则创建一个CreateConnectionTask去创建Connection
    // 否则，唤醒CreateConnectionThread
    // 调用该方法会触发创建新Connection
    private void emptySignal() {
        // 单线程创建新Connection逻辑
        if (createScheduler == null) {
            empty.signal();
            return;
        }

        // 后面都是采用线程池方式创建新Connection逻辑

        if (createTaskCount >= maxCreateTaskCount) {    // maxCreateTaskCount默认为3
            return;
        }

        if (activeCount + poolingCount + createTaskCount >= maxActive) {
            return;
        }

        createTaskCount++;
        CreateConnectionTask task = new CreateConnectionTask();
        this.createSchedulerFuture = createScheduler.submit(task);
    }
}














