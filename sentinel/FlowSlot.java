// 核心限流Slot
class FlowSlot{

	public void entry(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count,
                      boolean prioritized, Object... args) {
        checkFlow(resourceWrapper, context, node, count, prioritized);	//检查是否触发限流

        fireEntry(context, resourceWrapper, node, count, prioritized, args);	//流转到下一Slot
    }

    void checkFlow(ResourceWrapper resource, Context context, DefaultNode node, int count, boolean prioritized) throws BlockException {
        // Flow rule map cannot be null.
        Map<String, List<FlowRule>> flowRules = FlowRuleManager.getFlowRuleMap();

        List<FlowRule> rules = flowRules.get(resource.getName());
        if (rules != null) {
            for (FlowRule rule : rules) {
                if (!canPassCheck(rule, context, node, count, prioritized)) {
                    throw new FlowException(rule.getLimitApp());
                }
            }
        }
    }

    boolean canPassCheck(FlowRule rule, Context context, DefaultNode node, int count, boolean prioritized) {
        return FlowRuleChecker.passCheck(rule, context, node, count, prioritized);
    }
}


// 限流判断类
final class FlowRuleChecker {

    static boolean passCheck(/*@NonNull*/ FlowRule rule, Context context, DefaultNode node, int acquireCount,
                                          boolean prioritized) {
        String limitApp = rule.getLimitApp();
        ..

        if (rule.isClusterMode()) {
            return passClusterCheck(rule, context, node, acquireCount, prioritized);
        }

        return passLocalCheck(rule, context, node, acquireCount, prioritized);
    }

    // 本地化限流
    private static boolean passLocalCheck(FlowRule rule, Context context, DefaultNode node, int acquireCount,
                                          boolean prioritized) {
        Node selectedNode = selectNodeByRequesterAndStrategy(rule, context, node);
        if (selectedNode == null) {
            return true;
        }

        return rule.getRater().canPass(selectedNode, acquireCount);
    }
}






















