public interface Subscriber<T> {
    void onSubscribe(Subscription var1);

    void onNext(T var1);

    void onError(Throwable var1);

    void onComplete();
}

public interface CoreSubscriber<T> extends Subscriber<T> {
	..
}