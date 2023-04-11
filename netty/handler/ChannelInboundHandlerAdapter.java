// ChannelInboundHandler默认实现
// 基本啥都没干，就是把事件往后传递
public class ChannelInboundHandlerAdapter << ChannelHandlerAdapter implements ChannelInboundHandler {

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }
}