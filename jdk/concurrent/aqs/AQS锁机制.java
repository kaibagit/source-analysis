// 总结：
// 1、尝试抢占，将aqs的state改为非0，并将exclusiveOwnerThread改为当前线程
// 2、抢占失败
// 2.1、将当前线程构建node，CAS插入到CLH队列尾部，直到成功
// 2.2.1、判断前置节点是否为head，是则尝试抢占
// 2.2.2、如果前置节点waitStatus为SIGNAL,则park当前线程，等待被unpark，unpark之后，重新执行2.2.1流程；非SIGNAL，则重新执行2.2.1流程






// 入口
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();    //将线程自己的interrupt设置为true
}

// 1、尝试抢占，自己实现
public boolean tryAcquire(int acquires) {
	// 通过cas修改state，如果成功从0修改为非0，则说明抢占成功
    if (compareAndSetState(0, 1)) {
    	// 标识抢占成功的线程
        setExclusiveOwnerThread(Thread.currentThread());
        return true;
    }
    return false;
}

// 2、抢占失败，创建Node，并插入队列，直到成功
// 返回之前的tail节点
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);

    // Try the fast path of enq; backup to full enq on failure
    // 一开始tail为Null
    // 这段逻辑只是enq()方法的优化，如果cas操作失败，最终还是要通过enq()方法插入到队列中
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;   //当前node指向tail节点
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }

    enq(node);
    return node;
}
// 重复cas操作，将node插入到队列尾部，直到成功
// 返回之前的tail节点
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) {    // tail为空表明还未初始化，初始化之后，tail和head就不再为空
            if (compareAndSetHead(new Node()))  // 尝试将head和tail从null替换成全新空节点
                tail = head;
        } else {    // tail非null说明已经被初始化了
            node.prev = t;  //当前node指向tail节点
            if (compareAndSetTail(t, node)) {   //尝试将tail替换成当前node
                t.next = node;  //替换成功，则把之前的tail指向当前节点
                return t;
            }
        }
        // 任何cas操作失败，则重试
    }
}


// 尝试获取锁，失败则park，一直循环，直到成功或者interrupt
// 参数node为当前线程对应的node
// if interrupted while waiting，返回true
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;    //默认为false，只要有一次interrupt，就永远为true
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {     //如果前一个节点为head，则尝试抢占一次
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())    //park当前线程
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);    // 如果抛出异常则取消锁的获取，进行出队(sync queue)操作
    }
}
// 参数node为当前线程对应的node
// 返回当前线程是否应该park
// 如果上个节点状态为 SIGNAL，则应该park
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /*
         * This node has already set status asking a release
         * to signal it, so it can safely park.
         */
        return true;
    if (ws > 0) {
        /*
         * Predecessor was cancelled. Skip over predecessors and
         * indicate retry.
         */
        // 一直往前查找，找到第一个非cancelled节点，并重新设置前后链接;相当于移除了中间的cancelled节点
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /*
         * waitStatus must be 0 or PROPAGATE.  Indicate that we
         * need a signal, but don't park yet.  Caller will need to
         * retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}

// park当前线程，并清除interupted状态
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
}

// 将当前Node置为head，并清空thread和prev字段，head的thred、prev永远为Null
private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
}














// 总结：
// 1、减少AQS的state计数
// 2、如果计数归零，说明全部占用已释放，找到后续非cancelled节点，并unpark其线程
public final boolean release(int arg) {
    if (tryRelease(arg)) {      //尝试释放占用，如果全部释放，则true
        Node h = head;
        if (h != null && h.waitStatus != 0)     //有后续节点在阻塞
            unparkSuccessor(h);     // unpark后续node线程
        return true;
    }
    return false;
}
// 如果占用全部被释放，state=0，则返回true
protected boolean tryRelease(int releases) {
    if (getState() == 0) throw new IllegalMonitorStateException();
    setExclusiveOwnerThread(null);
    setState(0);
    return true;
}

// 将当前Node的waitStatus改为0，并且unpark后续node的线程
private void unparkSuccessor(Node node) {
    /*
     * If status is negative (i.e., possibly needing signal) try
     * to clear in anticipation of signalling.  It is OK if this
     * fails or if status is changed by waiting thread.
     */
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    /*
     * Thread to unpark is held in successor, which is normally
     * just the next node.  But if cancelled or apparently null,
     * traverse backwards from tail to find the actual
     * non-cancelled successor.
     */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}