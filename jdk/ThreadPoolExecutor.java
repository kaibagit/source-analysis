
class ThreadPoolExecutor{

	// RUNNING:  Accept new tasks and process queued tasks
	// SHUTDOWN: Don't accept new tasks, but process queued tasks
	// STOP：	 Don't accept new tasks, don't process queued tasks,and interrupt in-progress tasks
	// TIDYING： All tasks have terminated, workerCount is zero,the thread transitioning to state TIDYING will run the terminated() hook method
	// TERMINATED：  terminated() has completed
	// 状态值从小到大排列
	private static final int RUNNING    = -1 << 29;	//1110 0000000000000000000000000000
    private static final int SHUTDOWN   =  0 << 29;
    private static final int STOP       =  1 << 29;	//0010 0000000000000000000000000000
    private static final int TIDYING    =  2 << 29;	//0100 0000000000000000000000000000
    private static final int TERMINATED =  3 << 29;	//0110 0000000000000000000000000000

    // 代表了the effective number of threads runState，也代表了线程池状态，将2个值合成一个Int值；最小的28位代表workerCount，另外4位代表状态
    // 初始值为RUNNING
	private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));		//ctlOf(int rs, int wc) { return rs | wc; }

	private final ReentrantLock mainLock = new ReentrantLock();

	// 所有的worker集合
	private final HashSet<Worker> workers = new HashSet<Worker>();

	/*
     * Proceed in 3 steps:
     *
     * 1. If fewer than corePoolSize threads are running, try to
     * start a new thread with the given command as its first
     * task.  The call to addWorker atomically checks runState and
     * workerCount, and so prevents false alarms that would add
     * threads when it shouldn't, by returning false.
     *
     * 2. If a task can be successfully queued, then we still need
     * to double-check whether we should have added a thread
     * (because existing ones died since last checking) or that
     * the pool shut down since entry into this method. So we
     * recheck state and if necessary roll back the enqueuing if
     * stopped, or start a new thread if there are none.
     *
     * 3. If we cannot queue task, then we try to add a new
     * thread.  If it fails, we know we are shut down or saturated
     * and so reject the task.
     */
	public void execute(Runnable command) {
		int c = ctl.get();
		// worker数量小于corePoolSize，尝试addWorker()
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        // 尝试往workQueue增加任务
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            // 重新检查线程池状态，如果不处在Running状态，则移除任务，并执行拒绝策略
            if (! isRunning(recheck) && remove(command))
                reject(command);
            // 线程池处于Running状态，并且worker数量为0，则addWorker()
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        else if (!addWorker(command, false))
            reject(command);
	}

	boolean addWorker(Runnable firstTask, boolean core){
		retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            。。。

            for (;;) {
                int wc = workerCountOf(c);
                // 如果worker数量达到CAPACITY，或者达到corePoolSize/maximumPoolSize，直返返回失败
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
               	// 将worker原子性+1，成功则继续往下执行；
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                // 检测线程池状态是否有过变化，如果没有，则继续在当前for循环重试；否则，调到外部for循环重试
                c = ctl.get();  // Re-read ctl
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
        	// 创建worker
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;

                // 获取锁
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    int rs = runStateOf(ctl.get());

                    // 如果线程池处于RUNNING状态，则workers.add(w)，并刷新largestPoolSize
                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {
                        。。。
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
	}
	// 增加workker失败回滚：
	// 1、将worker从集合中移除
	// 2、减少worker计数
	// 3、tryTerminate()
	private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    // 供Worker调用
	void runWorker(Worker w){
		Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
        	// 一直循环，如果task=null，则从workQueue取
        	// 如果从workQueue取出为Null，则执行processWorkerExit()
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);	//拓展点
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);	//拓展点
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
	}

	public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }
}

class ThreadPoolExecutor.Worker implements Runnable{

	/** Thread this worker is running in.  Null if factory fails. */
    final Thread thread;
    /** Initial task to run.  Possibly null. */
    Runnable firstTask;

	Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }

	public void run() {
		runWorker(this);	//调用ThreadPoolExecutor#runWorker(w)
    }
}