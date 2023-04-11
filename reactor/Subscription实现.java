static final class ScalarSubscription<T>
			implements Fuseable.SynchronousSubscription<T>, InnerProducer<T> {

	final CoreSubscriber<? super T> actual;

	final T value;

	public void request(long n) {
		..
		Subscriber<? super T> a = actual;
		a.onNext(value);
		if(once != 2) {
			a.onComplete();
		}
		..
	}
}