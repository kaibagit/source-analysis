class PutKVConfigRequestHeader{
	@CFNotNull
    private String namespace;
    @CFNotNull
    private String key;
    @CFNotNull
    private String value;
}

class KVConfigManager{

	private final HashMap<String/* Namespace */, HashMap<String/* Key */, String/* Value */>> configTable =
        new HashMap<String, HashMap<String, String>>();

    public void putKVConfig(final String namespace, final String key, final String value) {
        。。。
        this.persist();
    }
}