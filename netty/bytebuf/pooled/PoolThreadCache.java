class PoolThreadCache{

	// 根据用户使用direct内存还是heap内存来确定使用哪个块
	// heapArena和directArena可能都非null
	final PoolArena<byte[]> heapArena;
	final PoolArena<ByteBuffer> directArena;

	//默认大小32的SubPageMemoryRegionCache[]，每个MemoryRegionCache的queue size为512
	private final MemoryRegionCache<byte[]>[] tinySubPageHeapCaches;
	//默认大小4的SubPageMemoryRegionCache[]，每个MemoryRegionCache的queue size为256
    private final MemoryRegionCache<byte[]>[] smallSubPageHeapCaches;
    //默认大小3的NormalMemoryRegionCache[]，每个NormalMemoryRegionCache的queue size为64
    private final MemoryRegionCache<byte[]>[] normalHeapCaches;

    private final MemoryRegionCache<ByteBuffer>[] tinySubPageDirectCaches;
    private final MemoryRegionCache<ByteBuffer>[] smallSubPageDirectCaches;
    private final MemoryRegionCache<ByteBuffer>[] normalDirectCaches;
}



// PoolThreadCache 内部类
abstract static class MemoryRegionCache<T>{
	private final int size;
    private final Queue<Entry<T>> queue;
    private final SizeClass sizeClass;

	MemoryRegionCache(int size, SizeClass sizeClass) {
        this.size = MathUtil.safeFindNextPositivePowerOfTwo(size);
        queue = PlatformDependent.newFixedMpscQueue(this.size);
        this.sizeClass = sizeClass;
    }
}

// PoolThreadCache 内部类
static class SubPageMemoryRegionCache<T> extends MemoryRegionCache<T> {

	SubPageMemoryRegionCache(int size, SizeClass sizeClass) {
        super(size, sizeClass);
    }
}