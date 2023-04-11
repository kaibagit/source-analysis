class SphU{

	// 检查resource的所有rule，如果符合，则抛出BlockException
	public static Entry entry(String name) throws BlockException {
        return Env.sph.entry(name, EntryType.OUT, 1, OBJECTS0);
    }
}