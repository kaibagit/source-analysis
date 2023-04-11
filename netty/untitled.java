//tiny级别的个数, 每次递增2^4b, tiny总共管理32个等级的小内存片:[16, 32, 48, ..., 496], 注意实际只有31个级别内存块
static final int numTinySubpagePools = 512 >>> 4;
//全局默认唯一的分配者, 见PooledByteBufAllocator.DEFAULT
final PooledByteBufAllocator parent;
// log(16M/8K) = 11,指的是normal类型的内存等级, 分别为[8k, 16k, 32k, ..., 16M]
private final int maxOrder;
//默认8k
final int pageSize;
//log(8k) =  13
final int pageShifts;
//默认16M
final int chunkSize;
//-8192
final int subpageOverflowMask;
//指的是small类型的内存等级: pageShifts - log(512) = 4,分别为[512, 1k, 2k, 4k]
final int numSmallSubpagePools;
 //small类型分31个等级[16, 32, ..., 512], 每个等级都可以存放一个链(元素为PoolSubpage), 可存放未分配的该范围的内存块
private final PoolSubpage<T>[] tinySubpagePools;
 //small类型分31个等级[512, 1k, 2k, 4k], 每个等级都可以存放一个链(元素为PoolSubpage), 可存放未分配的该范围的内存块
private final PoolSubpage<T>[] smallSubpagePools;//存储1024-8096大小的内存
 //存储chunk(16M)使用率的内存块, 不同使用率的chunk, 存放在不同的对象中
private final PoolChunkList<T> q050;
private final PoolChunkList<T> q025;   //存储内存利用率25-75%的chunk
private final PoolChunkList<T> q000;   //存储内存利用率1-50%的chunk
private final PoolChunkList<T> qInit;  //存储内存利用率0-25%的chunk
private final PoolChunkList<T> q075;    //存储内存利用率75-100%的chunk
private final PoolChunkList<T> q100;   //存储内存利用率100%的chunk
// Number of thread caches backed by this arena. 该PoolArea被多少线程引用。
final AtomicInteger numThreadCaches = new AtomicInteger();





private void allocate(PoolThreadCache cache, PooledByteBuf<T> buf, final int reqCapacity) {
   final int normCapacity = normalizeCapacity(reqCapacity);
    // capacity < pageSize   小于8k
   if (isTinyOrSmall(normCapacity)) {
       int tableIdx;
       PoolSubpage<T>[] table;
       boolean tiny = isTiny(normCapacity);
       if (tiny) { // < 512
            //若从缓冲中取得该值
           if (cache.allocateTiny(this, buf, reqCapacity, normCapacity)) {
               // was able to allocate out of the cache so move on
               return;
           }
           tableIdx = tinyIdx(normCapacity);
           table = tinySubpagePools;
       } else {  //small
           if (cache.allocateSmall(this, buf, reqCapacity, normCapacity)) {
               // was able to allocate out of the cache so move on
               return;
           }
           tableIdx = smallIdx(normCapacity);
           table = smallSubpagePools;
       }
       final PoolSubpage<T> head = table[tableIdx];
       /**
        * Synchronize on the head. This is needed as {@link PoolChunk#allocateSubpage(int)} and
        * {@link PoolChunk#free(long)} may modify the doubly linked list as well.
        */
        //小于8k的
       synchronized (head) {
            //如果分配完会从当前级别链上去掉
           final PoolSubpage<T> s = head.next;
            ///该型号的tiny的内存已经分配的有一个了
           if (s != head) {
               assert s.doNotDestroy && s.elemSize == normCapacity;
               long handle = s.allocate();//高32放着一个PoolSubpage里面哪段的哪个，低32位放着哪个叶子节点
               assert handle >= 0;
               s.chunk.initBufWithSubpage(buf, handle, reqCapacity);
               incTinySmallAllocation(tiny);
               return;//如果从链中找到就返回，
           }
       }
       //没有找到的话，就从Poolpage中分一个
       synchronized (this) {
           //说明head并没有分配值，是第一次分配。
           allocateNormal(buf, reqCapacity, normCapacity);
       }
       incTinySmallAllocation(tiny);
       return;
   }
   if (normCapacity <= chunkSize) { //小于16M
       if (cache.allocateNormal(this, buf, reqCapacity, normCapacity)) {  //cache=PoolThreadCache,本地是否已经有了
           // was able to allocate out of the cache so move on
           return;
       }
       synchronized (this) {
           allocateNormal(buf, reqCapacity, normCapacity);
           ++allocationsNormal;
       }
   } else {
       // Huge allocations are never served via the cache so just call allocateHuge
       allocateHuge(buf, reqCapacity); //大于16M，则分配大内存
   }
}






//该PoolChunk所属的PoolArena, 上层PoolArena控制着在哪块PoolArena上分配
final PoolArena<T> arena;。
//对外内存: DirectByteBuffer, 堆内内存 byte[]。
final T memory;
// 是内存池还是非内存池方式
final boolean unpooled;
private final byte[] memoryMap;  //PoolChunk的物理视图是连续的PoolSubpage,用PoolSubpage保持，而memoryMap是所有PoolSubpage的逻辑映射，映射为一颗平衡二叉数，用来标记每一个PoolSubpage是否被分配。下文会更进一步说明
private final byte[] depthMap;    //而depthMap中保存的值表示各个id对应的深度，是个固定值，初始化后不再变更。
//与叶子节点个数相同, 一个叶子节点可以映射PoolSupage中一个元素, 若叶子节点与该元素完成了映射, 说明该叶子节点已经被分配出去了
private final PoolSubpage<T>[] subpages;
/** Used to determine if the requested capacity is equal to or greater than pageSize. */
//用来判断申请的内存是否超过pageSize大小
private final int subpageOverflowMask;
//每个PoolSubpage的大小，默认为8192个字节（8K)
private final int pageSize;
//pageSize 2的 pageShifts幂
private final int pageShifts;
// 平衡二叉树的深度，
private final int maxOrder;
//PoolChunk的总内存大小,chunkSize =   (1<<maxOrder) * pageSize。
private final int chunkSize;
// PoolChunk由maxSubpageAllocs个PoolSubpage组成, 默认2048个。
private final int maxSubpageAllocs;
/** Used to mark memory as unusable */
//标记为已被分配的值，该值为 maxOrder + 1=12, 当memoryMap[id] = unusable时，则表示id节点已被分配
private final byte unusable;
//当前PoolChunk剩余可分配内存, 初始为16M。
private int freeBytes;
//一个PoolChunk分配后，会根据其使用率挂在一个PoolChunkList中(q050, q025...)
PoolChunkList<T> parent;