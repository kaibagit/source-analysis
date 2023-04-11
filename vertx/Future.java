

interface AsyncResult{

}

interface Future<T> extends AsyncResult<T>, Handler<AsyncResult<T>> {
}

class FutureImpl<T> implements Future<T>, Handler<AsyncResult<T>>{
  private boolean failed;
  private boolean succeeded;
  private Handler<AsyncResult<T>> handler;
  private T result;
  private Throwable throwable;

  public void complete(T result) {
    if (!tryComplete(result)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  public boolean tryComplete(T result) {
    Handler<AsyncResult<T>> h;
    // 设置future状态
    synchronized (this) {
      if (succeeded || failed) {
        return false;
      }
      this.result = result;
      succeeded = true;
      h = handler;
    }
    // 触发回调
    if (h != null) {
      h.handle(this);
    }
    return true;
  }

  // 用于桥接，将其他的future结果桥接到自己身上
  public Handler<AsyncResult<T>> completer() {
    return this;
  }
  public void handle(Future<T> ar) {
    if (ar.succeeded()) {
      complete(ar.result());
    } else {
      fail(ar.cause());
    }
  }

  //setHandler，该handler的逻辑为：
  //如果成功，交由hander处理；失败，则这是next的结果
  default <U> Future<U> compose(Handler<T> handler, Future<U> next) {
    setHandler(ar -> {
      if (ar.succeeded()) {
        try {
          handler.handle(ar.result());
        } catch (Throwable err) {
          if (next.isComplete()) {
            throw err;
          }
          next.fail(err);
        }
      } else {
        next.fail(ar.cause());
      }
    });
    return next;
  }

  //setHandler，将自身future的结果交由mapper处理
  //如果本身future处理成功，则将成功结果交给mapper处理，并创建新的future去承接结果，然后将该future返回；
  //如果失败，将失败结果set到新的future并返回
  default <U> Future<U> compose(Function<T, Future<U>> mapper) {
	。。。
    Future<U> ret = Future.future();
    setHandler(ar -> {
      if (ar.succeeded()) {
        Future<U> apply;
        try {
          apply = mapper.apply(ar.result());
        } catch (Throwable e) {
          ret.fail(e);
          return;
        }
        apply.setHandler(ret);
      } else {
        ret.fail(ar.cause());
      }
    });
    return ret;
  }
}

// T是入参，R是结果
@FunctionalInterface
public interface Function<T, R> {
	R apply(T t);
}
