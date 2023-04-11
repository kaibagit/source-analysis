// 选主流程：
// 在指定目录上创建临时顺序节点，并创建BackgroundCallback对象进行监听
// 在创建节点成功后，BackgroundCallback会触发并调用event.getChildren()，然后对这些子节点进行排序，如果发现自己是第一个，则说明自己为主节点，选主成功
// 然后继续监听指定目录的事件，如果有变更，继续获取所有子节点排序，重新选主。

class LeaderLatch {

	private final AtomicBoolean hasLeadership = new AtomicBoolean(false);		//当前是否为leader标识
	private final String latchPath;		//适用方在创建LeaderLatch时指定的路径名

	private final ConnectionStateListener listener = new ConnectionStateListener()
    {
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState)
        {
            handleStateChange(newState);
        }
    };
  private void handleStateChange(ConnectionState newState)
    {
        switch ( newState )
        {
            default:
            {
                // NOP
                break;
            }

            case RECONNECTED:
            {
                try
                {
                    reset();
                }
                catch ( Exception e )
                {
                    ThreadUtils.checkInterrupted(e);
                    log.error("Could not reset leader latch", e);
                    setLeadership(false);
                }
                break;
            }

            case SUSPENDED:
            case LOST:
            {
                setLeadership(false);
                break;
            }
        }
    }

	public void start() throws Exception {
	    Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED), "Cannot be started more than once");

	    startTask.set(AfterConnectionEstablished.execute(client, new Runnable()
	            {
	                @Override
	                public void run()
	                {
	                    try
	                    {
	                        internalStart();
	                    }
	                    finally
	                    {
	                        startTask.set(null);
	                    }
	                }
	            }));
    }

    private synchronized void internalStart()
    {
        if ( state.get() == State.STARTED )
        {
            client.getConnectionStateListenable().addListener(listener);
            try
            {
                reset();
            }
            catch ( Exception e )
            {
                ThreadUtils.checkInterrupted(e);
                log.error("An error occurred checking resetting leadership.", e);
            }
        }
    }

    // 选主过程核心方法
    void reset() throws Exception
    {
        setLeadership(false);
        setNode(null);

        BackgroundCallback callback = new BackgroundCallback()
        {
            @Override
            public void processResult(CuratorFramework client, CuratorEvent event) throws Exception
            {
                if ( debugResetWaitLatch != null )
                {
                    debugResetWaitLatch.await();
                    debugResetWaitLatch = null;
                }

                if ( event.getResultCode() == KeeperException.Code.OK.intValue() )
                {
                    setNode(event.getName());
                    if ( state.get() == State.CLOSED )
                    {
                        setNode(null);
                    }
                    else
                    {
                    		// 创建临时顺序节点成功后，获取该目录下所有的子节点，然后判断自己是否leader
                        getChildren();
                    }
                }
                else
                {
                    log.error("getChildren() failed. rc = " + event.getResultCode());
                }
            }
        };
        client.create().creatingParentContainersIfNeeded().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).inBackground(callback).forPath(ZKPaths.makePath(latchPath, LOCK_NAME), LeaderSelector.getIdBytes(id));
        // id默认值为""，这里将id内容转化为bytes，并作为节点data
        // ZKPaths.makePath()方法的结果类似于：/docking-accept-forecast/leader/latch-
        // forPath()方法最后会把path变成类似于"/docking-accept-forecast/leader/_c_9d4ac1be-709b-4a11-894f-8f8289de35c3-latch-"
    }
    private void getChildren() throws Exception
    {
        BackgroundCallback callback = new BackgroundCallback()
        {
            @Override
            public void processResult(CuratorFramework client, CuratorEvent event) throws Exception
            {
                if ( event.getResultCode() == KeeperException.Code.OK.intValue() )
                {
                    checkLeadership(event.getChildren());
                }
            }
        };
        client.getChildren().inBackground(callback).forPath(ZKPaths.makePath(latchPath, null));
    }
    private void checkLeadership(List<String> children) throws Exception
    {
        final String localOurPath = ourPath.get();
        // 1、将children节点排序
        List<String> sortedChildren = LockInternals.getSortedChildren(LOCK_NAME, sorter, children);
        // 2、找到自己节点在排序后的children的索引位置，如果为0即第一个，则为Leader
        int ourIndex = (localOurPath != null) ? sortedChildren.indexOf(ZKPaths.getNodeFromPath(localOurPath)) : -1;
        if ( ourIndex < 0 )
        {
            log.error("Can't find our node. Resetting. Index: " + ourIndex);
            reset();
        }
        else if ( ourIndex == 0 )
        {
            setLeadership(true);
        }
        else
        {
            ......
        }
    }
}

class CreateBuilderImpl {
	public String forPath(final String givenPath, byte[] data) throws Exception
    {
        if ( compress )
        {
            data = client.getCompressionProvider().compress(givenPath, data);
        }

        // 当givenPath="/docking-accept-forecast/leader/latch-"
        // adjustedPath="/docking-accept-forecast/leader/_c_9d4ac1be-709b-4a11-894f-8f8289de35c3-latch-"
        final String adjustedPath = adjustPath(client.fixForNamespace(givenPath, createMode.isSequential()));

        String returnPath = null;
        if ( backgrounding.inBackground() )
        {
            pathInBackground(adjustedPath, data, givenPath);
        }
        else
        {
            String path = protectedPathInForeground(adjustedPath, data);
            returnPath = client.unfixForNamespace(path);
        }
        return returnPath;
    }
}