class PoolArena{

	private final PoolSubpage<T>[] tinySubpagePools;
    private final PoolSubpage<T>[] smallSubpagePools;

    private final PoolChunkList<T> q050;
    private final PoolChunkList<T> q025;
    private final PoolChunkList<T> q000;
    private final PoolChunkList<T> qInit;
    private final PoolChunkList<T> q075;
    private final PoolChunkList<T> q100;

    protected PoolArena(PooledByteBufAllocator parent, int pageSize,
          int maxOrder, int pageShifts, int chunkSize, int cacheAlignment) {
    	// 初始化tinySubpagePools，长度为32，数组内元素的pageSize默认为8k
    	// 初始化smallSubpagePools，长度为4，数组内元素的pageSize默认为8k
    	..
    }

	private void allocate(PoolThreadCache cache, PooledByteBuf<T> buf, final int reqCapacity) {
		final int normCapacity = normalizeCapacity(reqCapacity);	//按照2的幂次向上取整
		if (isTinyOrSmall(normCapacity)) { // capacity < pageSize

			//
			// 总的原则：先尝试从PoolThreadCache中分配，如果分配失败，再从PoolArena分配内存
			// PoolArena分配内存，先尝试从tinySubpagePools/smallSubpagePools中查找，如果没找到，则直接在PoolChunk分配

			int tableIdx;				// 应该在tinySubpagePools/smallSubpagePools数组的索引处，分配内存
            PoolSubpage<T>[] table;		// 确定应该从tinySubpagePools或者smallSubpagePools分配内存
			boolean tiny = isTiny(normCapacity);
            if (tiny) { // < 512
                if (cache.allocateTiny(this, buf, reqCapacity, normCapacity)) {
                    return;
                }
                tableIdx = tinyIdx(normCapacity);
                table = tinySubpagePools;
            }
            ..
            final PoolSubpage<T> head = table[tableIdx];	// 找到用于分配的PoolSubpage链表head
            synchronized (head) {
            	// 尝试在PoolSubpage链表上分配内存，成功直接return
                ..
            }
            synchronized (this) {
                allocateNormal(buf, reqCapacity, normCapacity);
            }

            incTinySmallAllocation(tiny);
            return;
		}
		if (normCapacity <= chunkSize) {
			// 先尝试从PoolThreadCache中分配
			if (cache.allocateNormal(this, buf, reqCapacity, normCapacity)) {
                return;
            }
            synchronized (this) {
            	// 从PoolArena分配内存
                allocateNormal(buf, reqCapacity, normCapacity);
                ++allocationsNormal;
            }
		} else{
			allocateHuge(buf, reqCapacity);
		}
	}


	private void allocateNormal(PooledByteBuf<T> buf, int reqCapacity, int normCapacity) {
		..
		// 创建新的PoolChunk
		PoolChunk<T> c = newChunk(pageSize, maxOrder, pageShifts, chunkSize);
        long handle = c.allocate(normCapacity);
        ..
        c.initBuf(buf, handle, reqCapacity);
        qInit.add(c);
	}
}


// tinySubpagePools/smallSubpagePools每个元素为PoolSubpage对象，而PoolSubpage对象又含有引用，从而形成链表
class PoolSubpage<T> {
	private final int pageSize;
	PoolSubpage<T> prev;
    PoolSubpage<T> next;

	PoolSubpage(int pageSize) {
        ..
        this.pageSize = pageSize;
        ..
    }
}