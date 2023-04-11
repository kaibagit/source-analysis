enum EntryFactory {
	abstract <K, V> ReferenceEntry<K, V> newEntry(
        Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next);
}