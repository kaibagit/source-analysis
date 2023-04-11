public interface SlotChainBuilder {
	ProcessorSlotChain build();
}


public class DefaultSlotChainBuilder implements SlotChainBuilder {
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();
        chain.addLast(new NodeSelectorSlot());
        chain.addLast(new ClusterBuilderSlot());
        chain.addLast(new LogSlot());
        chain.addLast(new StatisticSlot());
        chain.addLast(new SystemSlot());
        chain.addLast(new AuthoritySlot());
        chain.addLast(new FlowSlot());
        chain.addLast(new DegradeSlot());
        return chain;
    }
}

// 应用启动前，需要事先在resource/META-INF/services中配置SlotChainBuilder

// entry = SphU.entry(KEY);核心流程：
// 1.根据resouce查找已经生成好的ProcessorSlotChain
// 2.然后调用chain.entry(context, resourceWrapper, null, count, prioritized, args);


// 责任链模式：
// 1.ProcessorSlotChain相当于netty的pipeline
// 2.Slot相当于netty的ChannelHandler
// 3.Slot在ProcessorSlotChain中形成单向链表，从ProcessorSlotChain#entry()方法进入，找到first Slot，并依次遍历下去
// 4.如果Slot在entry()执行过程中抛出异常，则终止流程


// StatisticSlot主要逻辑：
// 1.更新Node的实时统计数据