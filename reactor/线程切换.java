publishOn切换线程机制：当调用Subscription.request()时，会生成一个Runnable，提交给线程池；Runnable执行onNext()和onComplete()




public abstract class Mono<T> implements Publisher<T> {

	public final Mono<T> publishOn(Scheduler scheduler) {
		..
		T value = block();
		return onAssembly(new MonoSubscribeOnValue<>(value, scheduler));
		..
	}

	public final Mono<T> subscribeOn(Scheduler scheduler) {
		if(this instanceof Callable) {
			if (this instanceof Fuseable.ScalarCallable) {
				try {
					T value = block();
					return onAssembly(new MonoSubscribeOnValue<>(value, scheduler));
				}
				catch (Throwable t) {
					//leave MonoSubscribeOnCallable defer error
				}
			}
			@SuppressWarnings("unchecked")
			Callable<T> c = (Callable<T>)this;
			return onAssembly(new MonoSubscribeOnCallable<>(c,
					scheduler));
		}
		return onAssembly(new MonoSubscribeOn<>(this, scheduler));
	}

}



final class MonoSubscribeOnValue<T> extends Mono<T> implements Scannable {

	final T value;

	final Scheduler scheduler;

	MonoSubscribeOnValue(@Nullable T value, Scheduler scheduler) {
		this.value = value;
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
	}

	public void subscribe(CoreSubscriber<? super T> actual) {
		T v = value;
		if (v == null) {
			..
		}
		else {
			actual.onSubscribe(new ScheduledScalar<>(actual, v, scheduler));
		}
	}
}


static final class ScheduledScalar<T>
			implements QueueSubscription<T>, InnerProducer<T>, Runnable {

	ScheduledScalar(CoreSubscriber<? super T> actual, T value, Scheduler scheduler) {
		this.actual = actual;
		this.value = value;
		this.scheduler = scheduler;
	}

	public void request(long n) {
		..
		Disposable f = scheduler.schedule(this);
		..
	}

	public void run() {
		..
		actual.onNext(value);
		actual.onComplete();
		..
	}
}