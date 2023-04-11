// 缓存工具类核心接口
public interface LoadingCache<K, V> extends Cache<K, V>, Function<K, V> {
	V get(K key) throws ExecutionException;
}



class LocalCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

	// 用户自己定义的数据加载实现
	final CacheLoader<? super K, V> defaultLoader;

	LocalCache(
      CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
	  ..
	  defaultLoader = loader;
	  ..
	  this.segments = newSegmentArray(segmentCount);
	  ..
  }

  V getOrLoad(K key) throws ExecutionException {
	  return get(key, defaultLoader);
	}
	V get(K key, CacheLoader<? super K, V> loader) {
    int hash = hash(checkNotNull(key));
    return segmentFor(hash).get(key, hash, loader);
  }

}

/**
 * Segments are specialized versions of hash tables. This subclass inherits from ReentrantLock
 * opportunistically, just to simplify some locking and avoid separate construction.
 */
static class Segment<K, V> extends ReentrantLock {

	V get(K key, int hash, CacheLoader<? super K, V> loader) {
		..
		return lockedGetOrLoad(key, hash, loader);
		..
	}

	V lockedGetOrLoad(K key, int hash, CacheLoader<? super K, V> loader) {
    ReferenceEntry<K, V> e;
		..
		boolean createNewEntry = true;
		lock();
		..
    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
    int index = hash & (table.length() - 1);
    ReferenceEntry<K, V> first = table.get(index);

    for (e = first; e != null; e = e.getNext()) {
      K entryKey = e.getKey();
      if (e.getHash() == hash && entryKey != null
          && map.keyEquivalence.equivalent(key, entryKey)) {
        valueReference = e.getValueReference();
        if (valueReference.isLoading()) {
          createNewEntry = false;
        } else {
          ..
        }
        break;
      }
    }

    if (createNewEntry) {
      loadingValueReference = new LoadingValueReference<K, V>();

      if (e == null) {
        e = newEntry(key, hash, first);
        e.setValueReference(loadingValueReference);
        table.set(index, e);
      } else {
        e.setValueReference(loadingValueReference);
      }
    }

    unlock();
    ..

    if (createNewEntry) {
    	..
      synchronized (e) {
        return loadSync(key, hash, loadingValueReference, loader);
      }
      ..
    } else {
      // The entry already exists. Wait for loading.
      return waitForLoadingValue(e, key, valueReference);
    }

	}
}








static class LocalManualCache<K, V> implements Cache<K, V>, Serializable {

	final LocalCache<K, V> localCache;

}


static class LocalLoadingCache<K, V> extends LocalManualCache<K, V> implements LoadingCache<K, V> {

	// 继承自LocalManualCache
	final LocalCache<K, V> localCache;

  @Override
	public V get(K key) throws ExecutionException {
      return localCache.getOrLoad(key);
    }

	LocalLoadingCache(CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
        super(new LocalCache(builder, (CacheLoader)Preconditions.checkNotNull(loader)), null);
    }
}







