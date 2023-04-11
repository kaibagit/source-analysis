class TopicConfigManager extends ConfigManager {
    // ConfigManager
    public boolean load() {
        String fileName = null;
        try {
            fileName = this.configFilePath();
            // 将文件内容读取到字符串
            String jsonString = MixAll.file2String(fileName);

            if (null == jsonString || jsonString.length() == 0) {
                return this.loadBak();
            } else {
                this.decode(jsonString);
                log.info("load {} OK", fileName);
                return true;
            }
        } catch (Exception e) {
            log.error("load [{}] failed, and try to load backup file", fileName, e);
            return this.loadBak();
        }
    }

    public void decode(String jsonString) {
        TopicConfigSerializeWrapper topicConfigSerializeWrapper =
            TopicConfigSerializeWrapper.fromJson(jsonString, TopicConfigSerializeWrapper.class);
        if (topicConfigSerializeWrapper != null) {
            this.topicConfigTable.putAll(topicConfigSerializeWrapper.getTopicConfigTable());
            this.dataVersion.assignNewOne(topicConfigSerializeWrapper.getDataVersion());
            this.printLoadDataWhenFirstBoot(topicConfigSerializeWrapper);
        }
    }
}

// broker的topic配置信息，会被序列化为json字符串，写入磁盘
class TopicConfigSerializeWrapper extends RemotingSerializable{
    private ConcurrentMap<String, TopicConfig> topicConfigTable =
        new ConcurrentHashMap<String, TopicConfig>();
    private DataVersion dataVersion = new DataVersion();
}
// broker中的topic配置信息
TopicConfig{
    private static final String SEPARATOR = " ";
    public static int defaultReadQueueNums = 16;
    public static int defaultWriteQueueNums = 16;
    private String topicName;
    private int readQueueNums = defaultReadQueueNums;
    private int writeQueueNums = defaultWriteQueueNums;
    private int perm = PermName.PERM_READ | PermName.PERM_WRITE;
    private TopicFilterType topicFilterType = TopicFilterType.SINGLE_TAG;
    private int topicSysFlag = 0;
    private boolean order = false;
}