public interface Publisher<T> {
    void subscribe(Subscriber<? super T> var1);
}

public interface CorePublisher<T> extends Publisher<T> {

	default Context currentContext(){
		return Context.empty();
	}

	void subscribe(CoreSubscriber<? super T> subscriber);
}