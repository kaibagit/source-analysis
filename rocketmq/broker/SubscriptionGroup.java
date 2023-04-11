// broker保存的SubscriptionGroup信息，会被序列化为json字符串，写入磁盘
class SubscriptionGroupManager extends ConfigManager {
	private final ConcurrentMap<String, SubscriptionGroupConfig> subscriptionGroupTable =
        new ConcurrentHashMap<String, SubscriptionGroupConfig>(1024);
    private final DataVersion dataVersion = new DataVersion();
}