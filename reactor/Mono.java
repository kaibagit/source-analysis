public abstract class Mono<T> implements CorePublisher<T> {

	public static <T> Mono<T> just(T data) {
		return onAssembly(new MonoJust<>(data));
	}

	public static <T> Mono<T> create(Consumer<MonoSink<T>> callback) {
	    return onAssembly(new MonoCreate<>(callback));
	}

	// 增加了onEachOperatorHook、GLOBAL_TRACE处理
	protected static <T> Mono<T> onAssembly(Mono<T> source) {
		..
		return source;
	}




	public final Disposable subscribe(Consumer<? super T> consumer) {
		..
		return subscribe(consumer, null, null);
	}
	public final Disposable subscribe(
			@Nullable Consumer<? super T> consumer,
			@Nullable Consumer<? super Throwable> errorConsumer,
			@Nullable Runnable completeConsumer) {
		return subscribe(consumer, errorConsumer, completeConsumer, null);
	}
	public final Disposable subscribe(
			@Nullable Consumer<? super T> consumer,
			@Nullable Consumer<? super Throwable> errorConsumer,
			@Nullable Runnable completeConsumer,
			@Nullable Consumer<? super Subscription> subscriptionConsumer) {
		return subscribeWith(new LambdaMonoSubscriber<>(consumer, errorConsumer,
				completeConsumer, subscriptionConsumer));
	}
	public final <E extends Subscriber<? super T>> E subscribeWith(E subscriber) {
		subscribe(subscriber);
		return subscriber;
	}
	public final void subscribe(Subscriber<? super T> actual) {
		onLastAssembly(this).subscribe(Operators.toCoreSubscriber(actual));
	}
}





