/**
 * 分配一个接收buffer，用来写入所有的输入数据
 * Allocates a new receive buffer whose capacity is probably large enough to read all inbound data and small enough
 * not to waste its space.
 */
interface RecvByteBufAllocator{
    Handle newHandle();
}

// RecvByteBufAllocator内部类
// The handle provides the actual operations and keeps the internal information which is
// required for predicting an optimal buffer capacity.
interface RecvByteBufAllocator.Handle{
	/**
     * Creates a new receive buffer whose capacity is probably large enough to read all inbound data and small
     * enough not to waste its space.
     */
    ByteBuf allocate(ByteBufAllocator alloc);

    // Reset any counters that have accumulated and recommend how many messages/bytes should be read for the next read loop.
    void reset(ChannelConfig config);
}



// 默认情况下，RecvByteBufAllocator的实现类
public class AdaptiveRecvByteBufAllocator extends DefaultMaxMessagesRecvByteBufAllocator{

}

// DefaultMaxMessagesRecvByteBufAllocator内部类
public abstract class MaxMessageHandle implements ExtendedHandle {

	public ByteBuf allocate(ByteBufAllocator alloc) {
        return alloc.ioBuffer(guess());
    }
    
}

















