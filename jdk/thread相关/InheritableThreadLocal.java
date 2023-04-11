// v1.8 InheritableThreadLocal
class Thread{
	ThreadLocal.ThreadLocalMap inheritableThreadLocals;		//每个thread维护一个ThreadLocalMap实例

	// Thread的构造方法，会调用init方法
	private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals{default:true}) {

		Thread parent = currentThread();

		..

		// 注意，在创建子进程的那一刻，子线程能继承的InheritableThreadLocal数据已经固定了，后续父线程对InheritableThreadLocal改动，都不影响子线程
		if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
	}
}

class ThreadLocal{
	static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }
}

// ThreadLocal 内部类
class ThreadLocalMap{
	//根据parentMap初始化，遍历整个map的数组，将里面的数据copy出来
	private ThreadLocalMap(ThreadLocalMap parentMap) {
        Entry[] parentTable = parentMap.table;
        int len = parentTable.length;
        setThreshold(len);
        table = new Entry[len];

        for (int j = 0; j < len; j++) {
            Entry e = parentTable[j];
            if (e != null) {
                ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                if (key != null) {
                    Object value = key.childValue(e.value);	//对于InheritableThreadLocal来说，等同于Object value = e.value;
                    Entry c = new Entry(key, value);
                    int h = key.threadLocalHashCode & (len - 1);
                    while (table[h] != null)
                        h = nextIndex(h, len);
                    table[h] = c;
                    size++;
                }
            }
    }
}



class InheritableThreadLocal extends ThreadLocal{
    //InheritableThreadLocal与ThreadLocal使用的最大的区别，在于使用的ThreadLocalMap对象不同
    //ThreadLocal使用的是thread.threadLocals
    //InheritableThreadLocal使用的是thread.inheritableThreadLocals

    ThreadLocalMap getMap(Thread t) {
        // ThreadLocal的实现为：
        // return t.threadLocals;
       return t.inheritableThreadLocals;
    }
}