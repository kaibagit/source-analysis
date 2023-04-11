基于版本：4.1.25.Final
EventLoop相关代码

// 继承关系：
//  interface:
EventLoop << EventLoopGroup << EventExecutorGroup << ScheduleExecutorService << ExecutorService << Executor
//  class:
NioEventLoop << SingleThreadEventLoop << SingleThreadEventExecutor << AbstractScheduledEventExecutor << AbstractEventExecutor << AbstractExecutorService



// 简单的ThreadFactory
class DefaultThreadFactory < ThreadFactory{

  public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
    // ...
  }

  Thread newThread(Runnable r){
    // ...
  }
}

// Default implementation which uses simple round-robin to choose next EventExecutor.（即选择NioEventLoop）
class DefaultEventExecutorChooserFactory{
}

interface EventLoopGroup{
  EventLoop next();
  //将Channel注册到EventLoopGroup
  ChannelFuture register(Channel channel);
  ChannelFuture register(ChannelPromise promise);
  ChannelFuture register(Channel channel, ChannelPromise promise);
}







































































// NioEventLoop内部类
class SelectorTuple {
    final Selector unwrappedSelector;
    final Selector selector;

    SelectorTuple(Selector unwrappedSelector) {
        this.unwrappedSelector = unwrappedSelector;
        this.selector = unwrappedSelector;
    }

    SelectorTuple(Selector unwrappedSelector, Selector selector) {
        this.unwrappedSelector = unwrappedSelector;
        this.selector = selector;
    }
}





abstract class SingleThreadEventLoop extends SingleThreadEventExecutor{
}


























class SingleThreadEventLoop extends SingleThreadEventExecutor{
  // AbstractEventExecutor
  private final EventExecutorGroup parent;

  protected SingleThreadEventLoop(EventLoopGroup parent, 
                                  Executor executor,
                                  boolean addTaskWakesUp, 
                                  int maxPendingTasks,
                                  RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
        tailTasks = newTaskQueue(maxPendingTasks);
  }

  protected SingleThreadEventExecutor(EventExecutorGroup parent, 
                                      Executor executor,
                                      boolean addTaskWakesUp, 
                                      int maxPendingTasks,
                                      RejectedExecutionHandler rejectedHandler) {
        super(parent);
        this.addTaskWakesUp = addTaskWakesUp;
        this.maxPendingTasks = Math.max(16, maxPendingTasks);
        this.executor = ObjectUtil.checkNotNull(executor, "executor");
        taskQueue = newTaskQueue(this.maxPendingTasks);
        rejectedExecutionHandler = ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler");
  }

  // SingleThreadEventExecutor
  protected abstract void run();

  // AbstractEventExecutor
  public boolean inEventLoop() {
      return inEventLoop(Thread.currentThread());
  }
  // SingleThreadEventExecutor
  public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
  }

  // SingleThreadEventExecutor
  private void startThread() {
      if (state == ST_NOT_STARTED) {
          if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
              try {
                  doStartThread();
              } catch (Throwable cause) {
                  STATE_UPDATER.set(this, ST_NOT_STARTED);
                  。。。
              }
          }
      }
  }
  // SingleThreadEventExecutor
  private void doStartThread() {
      assert thread == null;
      // 从绑定的executor中获取线程执行
      executor.execute(new Runnable() {

          // 所有EventLoop的入口
          @Override
          public void run() {
              thread = Thread.currentThread();
              if (interrupted) {
                  thread.interrupt();
              }

              boolean success = false;
              updateLastExecutionTime();
              try {
                  SingleThreadEventExecutor.this.run();   //核心逻辑
                  success = true;
              } catch (Throwable t) {
                  logger.warn("Unexpected exception from an event executor: ", t);
              } finally {
                  for (;;) {
                      int oldState = state;
                      if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER.compareAndSet(
                              SingleThreadEventExecutor.this, oldState, ST_SHUTTING_DOWN)) {
                          break;
                      }
                  }

                  // Check if confirmShutdown() was called at the end of the loop.
                  if (success && gracefulShutdownStartTime == 0) {
                      logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " +
                              SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " +
                              "before run() implementation terminates.");
                  }

                  try {
                      // Run all remaining tasks and shutdown hooks.
                      for (;;) {
                          if (confirmShutdown()) {
                              break;
                          }
                      }
                  } finally {
                      try {
                          cleanup();
                      } finally {
                          STATE_UPDATER.set(SingleThreadEventExecutor.this, ST_TERMINATED);
                          threadLock.release();
                          if (!taskQueue.isEmpty()) {
                              logger.warn(
                                      "An event executor terminated with " +
                                              "non-empty task queue (" + taskQueue.size() + ')');
                          }

                          terminationFuture.setSuccess(null);
                      }
                  }
              }
          }
      });
  }
}












class SelectedSelectionKeySet{
  SelectionKey[] keys;
  int size;

  SelectedSelectionKeySet() {
      keys = new SelectionKey[1024];
  }
}







ChannelInitializer < ChannelInboundHandlerAdapter{
	public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        initChannel((C) ctx.channel());
        ctx.pipeline().remove(this);
        ctx.fireChannelRegistered();
    }
}


