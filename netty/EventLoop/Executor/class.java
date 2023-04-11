abstract class SingleThreadEventExecutor{

    // 继承自AbstractEventExecutor
    public boolean inEventLoop() {
        return inEventLoop(Thread.currentThread());
    }
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

}