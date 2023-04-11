// 路由信息，包含：
// broker信息
class RouteInfoManager{

	private final HashMap<String/* topic */, List<QueueData>> topicQueueTable;
    private final HashMap<String/* brokerName */, BrokerData> brokerAddrTable;
    private final HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;
    private final HashMap<String/* brokerAddr */, BrokerLiveInfo> brokerLiveTable;
    private final HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;

    // 查询执行broker信息
    public DataVersion queryBrokerTopicConfig(final String brokerAddr) {
        BrokerLiveInfo prev = this.brokerLiveTable.get(brokerAddr);
        if (prev != null) {
            return prev.getDataVersion();
        }
        return null;
    }

    // 查询topic路由信息，对应RequestCode.GET_ROUTEINTO_BY_TOPIC
    public TopicRouteData pickupTopicRouteData(final String topic) {
        TopicRouteData topicRouteData = new TopicRouteData();
        。。。
        topicRouteData.setBrokerDatas(brokerDataList);
        。。。
        List<QueueData> queueDataList = this.topicQueueTable.get(topic);
        。。。
        topicRouteData.setQueueDatas(queueDataList);
        。。。
        queueDataList.each |QueueData qd|{
            BrokerData brokerData = this.brokerAddrTable.get(qd.getBrokerName());
            。。。
            brokerDataList.add(brokerDataClone);
            。。。
        }
        。。。
    }

    // 对应RequestCode.GET_BROKER_CLUSTER_INFO
    public byte[] getAllClusterInfo() {
        ClusterInfo clusterInfoSerializeWrapper = new ClusterInfo();
        clusterInfoSerializeWrapper.setBrokerAddrTable(this.brokerAddrTable);
        clusterInfoSerializeWrapper.setClusterAddrTable(this.clusterAddrTable);
        return clusterInfoSerializeWrapper.encode();
    }
}

class BrokerLiveInfo{

	private long lastUpdateTimestamp;
    private DataVersion dataVersion;
    private Channel channel;
    private String haServerAddr;

}

class DataVersion{
	private long timestamp = System.currentTimeMillis();
    private AtomicLong counter = new AtomicLong(0);
}


class QueueData{
    private String brokerName;
    private int readQueueNums;
    private int writeQueueNums;
    private int perm;
    private int topicSynFlag;
}