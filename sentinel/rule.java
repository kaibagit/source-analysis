public interface Rule {
	boolean passCheck(Context context, DefaultNode node, int count, Object... args);
}


// FlowRuleManager，管理所有的Rule
public class FlowRuleManager {

	// 数据来源于currentProperty属性
	private static final Map<String/** resource **/, List<FlowRule>/** 该resource的限流规则列表**/> flowRules = new ConcurrentHashMap<String, List<FlowRule>>();

	private final static FlowPropertyListener listener = new FlowPropertyListener();
	private static SentinelProperty<List<FlowRule>> currentProperty = new DynamicSentinelProperty<List<FlowRule>>();

	static {
		// 给currentProperty配置listener
        currentProperty.addListener(listener);
        scheduler.scheduleAtFixedRate(new MetricTimerListener(), 0, 1, TimeUnit.SECONDS);
    }

	// 入口
	public static void loadRules(List<FlowRule> rules) {
        currentProperty.updateValue(rules);
    }

	// 监听器实现，
    private static final class FlowPropertyListener implements PropertyListener<List<FlowRule>> {

        @Override
        public void configUpdate(List<FlowRule> value) {
            Map<String, List<FlowRule>> rules = loadFlowConf(value);
            if (rules != null) {
                flowRules.clear();
                flowRules.putAll(rules);
            }
            RecordLog.info("[FlowRuleManager] Flow rules received: " + flowRules);
        }

        @Override
        public void configLoad(List<FlowRule> conf) {
            Map<String, List<FlowRule>> rules = loadFlowConf(conf);
            if (rules != null) {
                flowRules.clear();
                flowRules.putAll(rules);
            }
            RecordLog.info("[FlowRuleManager] Flow rules loaded: " + flowRules);
        }

    }

    // 将List<FlowRule>按照FlowRule.resource归类，封装成Map
    private static Map<String, List<FlowRule>> loadFlowConf(List<FlowRule> list) {                                                 
	    ...                                                                                                      
	}                                                                                                                              
}


// 其实就是针对某一个值，增加了变更时的lisner机制
public class DynamicSentinelProperty<T> {

	protected Set<PropertyListener<T>> listeners = Collections.synchronizedSet(new HashSet<PropertyListener<T>>());
    private T value = null;


    // 核心方法
    public void updateValue(T newValue) {
        if (isEqual(value, newValue)) {
            return;
        }
        RecordLog.info("[DynamicSentinelProperty] Config will be updated to: " + newValue);

        value = newValue;
        for (PropertyListener<T> listener : listeners) {
            listener.configUpdate(newValue);
        }

    }
}