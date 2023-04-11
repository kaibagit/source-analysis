final class LambdaMonoSubscriber<T> implements InnerConsumer<T>, Disposable {

	final Consumer<? super T>            consumer;
	final Consumer<? super Throwable>    errorConsumer;
	final Runnable                       completeConsumer;
	final Consumer<? super Subscription> subscriptionConsumer;

	LambdaMonoSubscriber(@Nullable Consumer<? super T> consumer,
			@Nullable Consumer<? super Throwable> errorConsumer,
			@Nullable Runnable completeConsumer,
			@Nullable Consumer<? super Subscription> subscriptionConsumer) {
		this.consumer = consumer;
		this.errorConsumer = errorConsumer;
		this.completeConsumer = completeConsumer;
		this.subscriptionConsumer = subscriptionConsumer;
	}

	public final void onSubscribe(Subscription s) {
		if (Operators.validate(subscription, s)) {
			this.subscription = s;

			if (subscriptionConsumer != null) {
				try {
					subscriptionConsumer.accept(s);
				}
				catch (Throwable t) {
					Exceptions.throwIfFatal(t);
					s.cancel();
					onError(t);
				}
			}
			else {
				s.request(Long.MAX_VALUE);
			}

		}
	}
}


final class StrictSubscriber<T> implements Scannable, CoreSubscriber<T>, Subscription {

	final Subscriber<? super T> actual;

	StrictSubscriber(Subscriber<? super T> actual) {
		this.actual = actual;
	}

	public void onSubscribe(Subscription s) {
		actual.onSubscribe(this);
		
		if (Operators.validate(this.s, s)) {

			actual.onSubscribe(this);

			if (Operators.setOnce(S, this, s)) {
				long r = REQUESTED.getAndSet(this, 0L);
				if (r != 0L) {
					s.request(r);
				}
			}
		}
		else {
			onError(new IllegalStateException("§2.12 violated: onSubscribe must be called at most once"));
		}
	}

	public void request(long n) {
		if (n <= 0) {
			cancel();
			onError(new IllegalArgumentException(
					"§3.9 violated: positive request amount required but it was " + n));
			return;
		}
		Subscription a = s;
		if (a != null) {
			a.request(n);
		}
		else {
			Operators.addCap(REQUESTED, this, n);
			a = s;
			if (a != null) {
				long r = REQUESTED.getAndSet(this, 0L);
				if (r != 0L) {
					a.request(n);
				}
			}
		}
	}
}