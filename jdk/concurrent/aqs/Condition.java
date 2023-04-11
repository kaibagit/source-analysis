public interface Condition {

    void await() throws InterruptedException;

    void awaitUninterruptibly();

    long awaitNanos(long nanosTimeout) throws InterruptedException;

    boolean await(long time, TimeUnit unit) throws InterruptedException;

    boolean awaitUntil(Date deadline) throws InterruptedException;

    void signal();

    void signalAll();
}


// AQS内部类
public class ConditionObject implements Condition {

     /** First node of condition queue. */
     private Node firstWaiter;
     /** Last node of condition queue. */
     private Node lastWaiter;





     /*
     * await相关逻辑
     */
     public final void await() throws InterruptedException {
       if (Thread.interrupted())
           throw new InterruptedException();
       Node node = addConditionWaiter();     //用当前线程构建node，并加入队列中
       int savedState = fullyRelease(node);  //调用AQS的release方法，释放所有占用
       int interruptMode = 0;
       // 如果在Condition队列里，则一直park，除非interrupt
       // 当signal之后，node会从Condition放入Sync队列，则跳出while循环
       while (!isOnSyncQueue(node)) {
           LockSupport.park(this);
           if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
               break;
       }
       // acquireQueued一直尝试去抢占锁
       if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
           interruptMode = REINTERRUPT;
       if (node.nextWaiter != null) // clean up if cancelled
           unlinkCancelledWaiters();
       if (interruptMode != 0)
           reportInterruptAfterWait(interruptMode);
     }

     private Node addConditionWaiter() {
       Node t = lastWaiter;
       // If lastWaiter is cancelled, clean out.
       if (t != null && t.waitStatus != Node.CONDITION) {
           unlinkCancelledWaiters();
           t = lastWaiter;
       }
       Node node = new Node(Thread.currentThread(), Node.CONDITION);
       if (t == null)
           firstWaiter = node;
       else
           t.nextWaiter = node;
       lastWaiter = node;
       return node;
     }

     final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }
    // 节点是否在CLH队列中
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)   //初始时，node.waitStatus == Node.CONDITION
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }







    /*
     * signal 相关逻辑
     */
    public final void signal() {
       if (!isHeldExclusively())
           throw new IllegalMonitorStateException();
       Node first = firstWaiter;
       if (first != null)
           doSignal(first);
   }

   private void doSignal(Node first) {
       do {
           // 移除firstWaiter节点，指向下一个节点
           if ( (firstWaiter = first.nextWaiter) == null)
               lastWaiter = null;
           first.nextWaiter = null;
       } while (!transferForSignal(first) &&
                (first = firstWaiter) != null);
   }
   // 被移除的firstWaiter节点加入CLH队列
   final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        // 如果设置失败，说明已经被取消，没必要再进入Sync队列了
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        Node p = enq(node);   //将Condition头节点重新加入CLH队列，q为之前CLH队列的tail节点
        int ws = p.waitStatus;
        // 如果执行成功，则将node加入到Sync队列中，enq会返回node的前继节点p。这里的if判断只有在p节点是取消状态或者设置p节点的状态为SIGNAL失败的时候才会执行unpark。
        // 什么时候compareAndSetWaitStatus(p, ws, Node.SIGNAL)会执行失败呢？如果p节点的线程在这时执行了unlock方法，就会调用unparkSuccessor方法，unparkSuccessor方法可能就将p的状态改为了0，那么执行就会失败。
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
   }
}







