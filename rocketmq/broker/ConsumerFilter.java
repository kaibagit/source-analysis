// broker保存的consumer使用的expression filter，会被序列化为json字符串，写入磁盘
class ConsumerFilterManager extends ConfigManager {
	private ConcurrentMap<String/*Topic*/, FilterDataMapByTopic>
        filterDataByTopic = new ConcurrentHashMap<String/*consumer group*/, FilterDataMapByTopic>(256);

    public static class FilterDataMapByTopic {
    	private ConcurrentMap<String/*consumer group*/, ConsumerFilterData>
            groupFilterData = new ConcurrentHashMap<String, ConsumerFilterData>();

        private String topic;
    }
}
class ConsumerFilterData {
	private String consumerGroup;
    private String topic;
    private String expression;
    private String expressionType;
    private transient Expression compiledExpression;
    private long bornTime;
    private long deadTime = 0;
    private BloomFilterData bloomFilterData;
    private long clientVersion;
}