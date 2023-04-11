class NioEventLoopGroup extends MultithreadEventLoopGroup{
  private final EventExecutorChooser chooser;
  // MultithreadEventExecutorGroup，Executor数组，每个Executor又是一个线程池
  private final EventExecutor[] children;

  public NioEventLoopGroup(int nThreads{default:0}, 
                          Executor executor, 
                          final SelectorProvider selectorProvider{default:SelectorProvider.provider()},
                          final SelectStrategyFactory selectStrategyFactory{default:DefaultSelectStrategyFactory.INSTANCE}) {
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
  }

  protected MultithreadEventExecutorGroup(int nThreads{default:CPU核数*2}, 
                                          Executor executor,
                                          EventExecutorChooserFactory chooserFactory{default:DefaultEventExecutorChooserFactory.INSTANCE}, 
                                          Object... args) {
    if (executor == null) {
        // 创建一个Executor，特性是每提交一个任务，都会创建一个新的threasd来运行
        executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
    }

    // 创建指定数量的NioEventLoop
    children = new EventExecutor[nThreads];
    for (int i = 0; i < nThreads; i ++) {
      ..
      children[i] = newChild(executor, args);
      ..
    }

    chooser = chooserFactory.newChooser(children);  //创建选择器，用于选择一个NioEventLoop去执行具体的event处理

    // 给NioEventLoop注册termination监听器
    final FutureListener<Object> terminationListener = new FutureListener<Object>() {
        @Override
        public void operationComplete(Future<Object> future) throws Exception {
            if (terminatedChildren.incrementAndGet() == children.length) {
                terminationFuture.setSuccess(null);
            }
        }
    };
    for (EventExecutor e: children) {
        e.terminationFuture().addListener(terminationListener);
    }

    // 将NioEventLoop组成只读Set
    Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
    Collections.addAll(childrenSet, children);
    readonlyChildren = Collections.unmodifiableSet(childrenSet);
  }



  // 创建新的EventLoop
  protected EventLoop newChild(Executor executor, Object... args) throws Exception {
          return new NioEventLoop(this, executor, 
                  (SelectorProvider) args[0],
                  ((SelectStrategyFactory) args[1]).newSelectStrategy(), 
                  (RejectedExecutionHandler) args[2]);
  }

  public ChannelFuture register(Channel channel) {
    return next().register(channel);  //NioEventLoop.register(channel)
  }

  // MultithreadEventLoopGroup
  public EventLoop next() {
    return (EventLoop) chooser.next();
  }
}










class NioEventLoop extends SingleThreadEventLoop{
  // 每个NioEventLoop都会绑定一个Selector，然后再将Channel注册到Selector上

  Selector selector;              //绑定的NIO Selector对象
  Selector unwrappedSelector;     //经netty封装过后的Selector对象，也有可能与selector是同一对象
  private final SelectorProvider provider;
  EventExecutorGroup parent;
  boolean addTaskWakesUp;
  ThreadProperties threadProperties;
  private final Queue<Runnable> taskQueue;

  // 控制决定阻塞的Selector.select()应该逃离selection process。
  // true表示应该中断select()，并wakeup()
  AtomicBoolean wakenUp = new AtomicBoolean();

  // io比率，默认为50，表示用于IO和非IO任务各占50%
  volatile int ioRatio = 50;

  NioEventLoop(NioEventLoopGroup parent, 
              Executor executor, 
              SelectorProvider selectorProvider,
              SelectStrategy strategy, 
              RejectedExecutionHandler rejectedExecutionHandler) {
        //SingleThreadEventLoop构造方法
        super(parent, executor, false, DEFAULT_MAX_PENDING_TASKS, rejectedExecutionHandler);

        provider = selectorProvider;
        final SelectorTuple selectorTuple = openSelector();   //获取selector，出于优化目的，该selector可能被netty包装了一次
        selector = selectorTuple.selector;
        unwrappedSelector = selectorTuple.unwrappedSelector;
        selectStrategy = strategy;
  }

  // SingleThreadEventLoop 方法
  public ChannelFuture register(Channel channel) {
      return register(new DefaultChannelPromise(channel, this));
  }
  public ChannelFuture register(final ChannelPromise promise) {
      promise.channel().unsafe().register(this, promise);
      return promise;
  }
  public ChannelFuture register(final Channel channel, final ChannelPromise promise) {
      channel.unsafe().register(this, promise);
      return promise;
  }

  // 执行入口
  protected void run() {
        for (;;) {
            try {
                // selectNowSupplier：返回selectNow()的值;
                // 如果hasTasks()==false，则返回SelectStrategy.SELECT
                // 如果hasTasks()==false，则表示taskQueue没有待执行任务，可以执行IO操作
                switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                    case SelectStrategy.CONTINUE:
                        continue;
                    // taskQueue没有待执行的task
                    case SelectStrategy.SELECT:
                        // 设置wakenUp=false
                        // 最终会调用到selector#selectNow()或者是selector#select(timeoutMillis)
                        select(wakenUp.getAndSet(false));
                        
                        if (wakenUp.get()) {
                            selector.wakeup();
                        }
                        // fall through
                    // taskQueue有待执行的task，直接往下执行
                    default:
                }

                ..
                final int ioRatio = this.ioRatio;
                if (ioRatio == 100) {
                    ..
                    processSelectedKeys();
                    ..
                    runAllTasks();
                } else {
                    // 优先执行IO操作，并计算IO操作总共耗时
                    final long ioStartTime = System.nanoTime();
                    ..
                    processSelectedKeys();
                    ..
                    final long ioTime = System.nanoTime() - ioStartTime;
                    // 按照ioRatio以及本次IO实际执行耗时，计算接下来运行taskQueue中任务的超时时间
                    // 在限定的超时时间内运行taskQueue中任务
                    runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            ..
        }
    }
    private void select(boolean oldWakenUp) throws IOException {
        ..
        int selectedKeys = selector.select(timeoutMillis);
        ..
  }

  // 从taskQueue中取出任务运行，并累计执行耗时
  // 当执行耗时达到timeoutNanos时，则break，从而退出该方法执行
  protected boolean runAllTasks(long timeoutNanos) {
    ..
  }

  // 将原始的selector封装成SelectorTuple
  private SelectorTuple openSelector() {
    Selector unwrappedSelector = provider.openSelector();
    ..
    final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();
    ..
    // 将selectedKeys注入到原生Selector对象内
    selectedKeysField.set(unwrappedSelector, selectedKeySet);
    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
    ..
    selectedKeys = selectedKeySet;
    ..
  }

  private void processSelectedKeys() {
      if (selectedKeys != null) {   //selectedKeys：一般在openSelector()创建SelectedSelectionKeySet实例，不为Null
          processSelectedKeysOptimized();
      } else {
          // 获取触发的SelectionKey进行处理
          processSelectedKeysPlain(selector.selectedKeys());
      }
  }
  private void processSelectedKeysOptimized() {
    selectedKeys.each |SelectionKey k|{
      ..
      final Object a = k.attachment();
      if (a instanceof AbstractNioChannel) {
          processSelectedKey(k, (AbstractNioChannel) a);
      } else {
          NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
          processSelectedKey(k, task);
      }
      ..
    }
  }
  private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    ..
    int readyOps = k.readyOps();

    if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
        int ops = k.interestOps();
        ops &= ~SelectionKey.OP_CONNECT;
        k.interestOps(ops);

        unsafe.finishConnect();
    }

    if ((readyOps & SelectionKey.OP_WRITE) != 0) {
        // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
        ch.unsafe().forceFlush();
    }

    if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
        unsafe.read();
    }
    ..
  }

  private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
      ..
  }



  taskQueue相关逻辑
  // 继承自SingleThreadEventExecutor
  private final Queue<Runnable> taskQueue;

  // 继承自SingleThreadEventExecutor
  // NIO Selector#select()相关api触发入口
  // SingleThreadEventExecutor
  // 需要判断当前是否在EventLoop线程中，如果不是，则启动从EventLoop绑定的executor获取线程并启动run()
  public void execute(Runnable task) {
      ..
      boolean inEventLoop = inEventLoop();
      addTask(task);
      if (!inEventLoop) {
          startThread();
          if (isShutdown() && removeTask(task)) {
              reject();
          }
      }

      if (!addTaskWakesUp && wakesUpForTask(task)) {
          wakeup(inEventLoop);
      }
  }
  protected void addTask(Runnable task) {
      ..
      if (!offerTask(task)) {
          reject(task);
      }
  }
  protected boolean hasTasks() {
      ..
      return !taskQueue.isEmpty();
  }

}