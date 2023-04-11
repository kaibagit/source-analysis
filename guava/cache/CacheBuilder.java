public final class CacheBuilder<K, V> {

	public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build(CacheLoader<? super K1, V1> loader) {
        this.checkWeightWithWeigher();
        return new LocalLoadingCache(this, loader);
    }
    
}