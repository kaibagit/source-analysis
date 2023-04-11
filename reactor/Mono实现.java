final class MonoJust<T> extends Mono<T> {

	public void subscribe(CoreSubscriber<? super T> actual) {
		//生成一个Subscription实例，并触发Subscriber#onSubscribe()方法
		actual.onSubscribe(Operators.scalarSubscription(actual, value));
	}
}

final class MonoMap<T, R> extends MonoOperator<T, R> {
	..
}