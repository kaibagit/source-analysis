class DefaultProcessorSlotChain{
	AbstractLinkedProcessorSlot<?> first;
	AbstractLinkedProcessorSlot<?> end = first;

	public void addLast(AbstractLinkedProcessorSlot<?> protocolProcessor) {
        end.setNext(protocolProcessor);
        end = protocolProcessor;
    }

    // 入口
    public void entry(Context context, ResourceWrapper resourceWrapper, Object t, int count, boolean prioritized, Object... args)
        throws Throwable {
        first.transformEntry(context, resourceWrapper, t, count, prioritized, args);
    }
}