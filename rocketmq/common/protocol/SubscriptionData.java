class SubscriptionData{

	public final static String SUB_ALL = "*";
    private boolean classFilterMode = false;
    private String topic;	//订阅的topic
    private String subString;	//订阅topic的tags字符串
    private Set<String> tagsSet = new HashSet<String>();	//如果subString不是*，则拆解成tagsSet
    private Set<Integer> codeSet = new HashSet<Integer>();
    private long subVersion = System.currentTimeMillis();
    private String expressionType = ExpressionType.TAG;

    @JSONField(serialize = false)
    private String filterClassSource;

}