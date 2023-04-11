/**
   * An entry in a reference map.
   *
   * Entries in the map can be in the following states:
   *
   * Valid:
   * - Live: valid key/value are set
   * - Loading: loading is pending
   *
   * Invalid:
   * - Expired: time expired (key/value may still be set)
   * - Collected: key/value was partially collected, but not yet cleaned up
   * - Unset: marked as unset, awaiting cleanup or reuse
   */
interface ReferenceEntry<K, V> {
	int getHash();
	K getKey();
	ValueReference<K, V> getValueReference();
   void setValueReference(ValueReference<K, V> valueReference);
	ReferenceEntry<K, V> getNext();
}
interface ValueReference<K, V> {
	V get();
	boolean isLoading();
}
static class LoadingValueReference<K, V> implements ValueReference<K, V> {
}