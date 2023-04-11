class ForkJoinPool{
	private static final sun.misc.Unsafe U;

    // runState bits: SHUTDOWN must be negative, others arbitrary powers of two
    private static final int  RSLOCK     = 1;
    private static final int  RSIGNAL    = 1 << 1;
    private static final int  STARTED    = 1 << 2;
    private static final int  STOP       = 1 << 29;
    private static final int  TERMINATED = 1 << 30;
    private static final int  SHUTDOWN   = 1 << 31;

    volatile int runState;

    // parallelism默认为CPU核数
    public ForkJoinPool(int parallelism,
                        ForkJoinWorkerThreadFactory factory,
                        UncaughtExceptionHandler handler,
                        boolean asyncMode) {
        this(checkParallelism(parallelism),
             checkFactory(factory),
             handler,
             asyncMode ? FIFO_QUEUE : LIFO_QUEUE,
             "ForkJoinPool-" + nextPoolId() + "-worker-");
        ..
    }

	public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        ..
        externalPush(task);
        return task;
    }

    final void externalPush(ForkJoinTask<?> task) {
        WorkQueue[] ws; WorkQueue q; int m;
        int r = ThreadLocalRandom.getProbe();
        int rs = runState;
        if ((ws = workQueues) != null && (m = (ws.length - 1)) >= 0 &&
            (q = ws[m & r & SQMASK]) != null && r != 0 && rs > 0 &&
            U.compareAndSwapInt(q, QLOCK, 0, 1)) {
            ForkJoinTask<?>[] a; int am, n, s;
            if ((a = q.array) != null &&
                (am = a.length - 1) > (n = (s = q.top) - q.base)) {
                int j = ((am & s) << ASHIFT) + ABASE;
                U.putOrderedObject(a, j, task);
                U.putOrderedInt(q, QTOP, s + 1);
                U.putIntVolatile(q, QLOCK, 0);
                if (n <= 1)
                    signalWork(ws, q);
                return;
            }
            U.compareAndSwapInt(q, QLOCK, 1, 0);
        }
        externalSubmit(task);
    }
    private void externalSubmit(ForkJoinTask<?> task) {
        ..
        for (;;) {
            WorkQueue[] ws; WorkQueue q; int rs, m, k;
            ..
            rs = runState
            ..
            if ((rs & STARTED) == 0) {
                ..
                workQueues = new WorkQueue[n];  //n=8
                ns = STARTED;
                ..
            }
            else if ((q = ws[k = r & m & SQMASK]) != null) {
                ..
                ForkJoinTask<?>[] a = q.array;
                ..
                boolean submitted = false;
                ..
                U.putOrderedObject(a, j, task);
                ..
                submitted = true;
                ..
                if (submitted) {
                    signalWork(ws, q);
                    return;
                }
                ..
            }
            else if (((rs = runState) & RSLOCK) == 0) { // create new queue
                q = new WorkQueue(this, null);
                q.hint = r;
                q.config = k | SHARED_QUEUE;
                q.scanState = INACTIVE;
                rs = lockRunState();           // publish index
                if (rs > 0 &&  (ws = workQueues) != null &&
                    k < ws.length && ws[k] == null)
                    ws[k] = q;                 // else terminated
                unlockRunState(rs, rs & ~RSLOCK);
            }
            ..
        }
        ..
    }
}



// ForkJoinPool内部类
static final class WorkQueue {
}






class ForkJoinTask{

}








class ForkJoinWorkerThread{
	final ForkJoinPool pool;                // the pool this thread works in
    final ForkJoinPool.WorkQueue workQueue; // work-stealing mechanics
	
	ForkJoinWorkerThread(ForkJoinPool pool) {
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }
}