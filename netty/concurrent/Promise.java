class DefaultPromise<V> extends AbstractFuture<V> implements Promise<V> {
	public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        ..
        synchronized (this) {
            addListener0(listener);
        }
        if (isDone()) {
            notifyListeners();
        }
        return this;
    }
    private void addListener0(GenericFutureListener<? extends Future<? super V>> listener) {
        if (listeners == null) {
            listeners = listener;
        } else if (listeners instanceof DefaultFutureListeners) {
            ((DefaultFutureListeners) listeners).add(listener);
        } else {
        	// DefaultFutureListeners底层是一个数组，每次增减都可能引起底层数组copy
            listeners = new DefaultFutureListeners((GenericFutureListener<?>) listeners, listener);
        }
    }
}



public interface Promise<V> extends Future<V> {
}

public interface Future<V> extends java.util.concurrent.Future<V> {
}