// 核心属性
private long nextFreeTicketMicros = 0L;   // 下次请求，允许放行的最小时间，只要请求时间>nextFreeTicketMicros，则放行
double storedPermits; // 累加的permits，如果请求量较少，会累加；最大值为maxPermits


// 核心代码：
public double acquire(int permits) {
    checkPermits(permits);
    long microsToWait;
    synchronized (mutex) {
      microsToWait = reserveNextTicket(permits, readSafeMicros());  //返回需要休眠的时间，>=0
    }
    ticker.sleepMicrosUninterruptibly(microsToWait);  // microsToWait大于0，则休眠对应时间
    return 1.0 * microsToWait / TimeUnit.SECONDS.toMicros(1L);
}



// 1、当前时间>nextFreeTicketMicros时：
// 即中间有一段留空时间，所以要计算在这段期间内，生成了多少的permits
private void resync(long nowMicros) {
    // if nextFreeTicket is in the past, resync to now
    if (nowMicros > nextFreeTicketMicros) {
      storedPermits = Math.min(maxPermits,
          storedPermits + (nowMicros - nextFreeTicketMicros) / stableIntervalMicros);
      nextFreeTicketMicros = nowMicros;
    }
}
// 当前时间>nextFreeTicketMicros时：
// 1.1、当permits有富余时：
private long reserveNextTicket(double requiredPermits, long nowMicros) {
    resync(nowMicros);
    long microsToNextFreeTicket = 0;
    double storedPermitsToSpend = requiredPermits;  //已存储的permits当中，被消耗的数量
    double freshPermits = requiredPermits - storedPermitsToSpend; // freshPermits =0

    long waitMicros = 0
        + (long) (freshPermits * stableIntervalMicros);   // waitMicros=0

    this.nextFreeTicketMicros = nowMicros + waitMicros;   // this.nextFreeTicketMicros = nowMicros
    this.storedPermits -= storedPermitsToSpend;   // 扣减调消耗的permits
    return microsToNextFreeTicket;
}
// 当前时间>nextFreeTicketMicros时：
// 1.2、当permits不足时：
private long reserveNextTicket(double requiredPermits, long nowMicros) {
    resync(nowMicros);
    long microsToNextFreeTicket = 0;
    double storedPermitsToSpend = this.storedPermits;
    double freshPermits = requiredPermits - storedPermitsToSpend; // 还欠缺的permits

    long waitMicros = 0
        + (long) (freshPermits * stableIntervalMicros);   // 需要等待的时间

    this.nextFreeTicketMicros = nowMicros + waitMicros;
    this.storedPermits -= storedPermitsToSpend;   // this.storedPermits = 0
    return microsToNextFreeTicket;
}

// 2、当前时间<=nextFreeTicketMicros时：
private long reserveNextTicket(double requiredPermits, long nowMicros) {
    long microsToNextFreeTicket = nextFreeTicketMicros - nowMicros;   //达到下次放行，所需要的时间
    double storedPermitsToSpend = 0;
    double freshPermits = requiredPermits - storedPermitsToSpend;   // freshPermits = requiredPermits

    long waitMicros = 0
        + (long) (freshPermits * stableIntervalMicros);   // waitMicros = 使用的这些令牌，所占用的时间

    this.nextFreeTicketMicros = nextFreeTicketMicros + waitMicros;  // 重新累计计算下次放行时间
    this.storedPermits -= storedPermitsToSpend;   // this.storedPermits = 0
    return microsToNextFreeTicket;
}



/**
 * 尝试
 */
public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
  long timeoutMicros = unit.toMicros(timeout);
  //checkPermits(permits);
  long microsToWait;
  synchronized (mutex) {
    long nowMicros = readSafeMicros();
    if (nextFreeTicketMicros > nowMicros + timeoutMicros) {
      // 超时了还是没有足够的permits，则直接返回false
      return false;
    } else {
      // 提前占用permits，并计算需要等待的时间
      microsToWait = reserveNextTicket(permits, nowMicros);
    }
  }
  ticker.sleepMicrosUninterruptibly(microsToWait);
  return true;
}























class RateLimiter{
    
    public static RateLimiter create(double permitsPerSecond) {
        /*
         * The default RateLimiter configuration can save the unused permits of up to one second.
         * This is to avoid unnecessary stalls in situations like this: A RateLimiter of 1qps,
         * and 4 threads, all calling acquire() at these moments:
         *
         * T0 at 0 seconds
         * T1 at 1.05 seconds
         * T2 at 2 seconds
         * T3 at 3 seconds
         *
         * Due to the slight delay of T1, T2 would have to sleep till 2.05 seconds,
         * and T3 would also have to sleep till 3.05 seconds.
         */
        return create(SleepingStopwatch.createFromSystemTimer(), permitsPerSecond);
    }

    static RateLimiter create(SleepingStopwatch stopwatch, double permitsPerSecond) {
        RateLimiter rateLimiter = new SmoothBursty(stopwatch, 1.0 /* maxBurstSeconds */);   //maxBurstSeconds = 1.0
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }
    
    public static RateLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        return create(SleepingStopwatch.createFromSystemTimer(), permitsPerSecond, warmupPeriod, unit);
    }
    
    static RateLimiter create(
      SleepingStopwatch stopwatch, double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        RateLimiter rateLimiter = new SmoothWarmingUp(stopwatch, warmupPeriod, unit);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }
    
    SleepingTicker ticker;
    long offsetNanos = Platform.systemNanoTime();
    long nextFreeTicketMicros = 0L; 
    double stableIntervalMicros;    //每个时间间隔（微妙）
    
    //SmoothRateLimiter
    double storedPermits;
    //SmoothRateLimiter
    double maxPermits;
    
    //SmoothWarmingUp
    private final long warmupPeriodMicros;
    
    public final void setRate(double permitsPerSecond) {
        synchronized (mutex()) {
          doSetRate(permitsPerSecond, stopwatch.readMicros());
        }
    }
    
    //SmoothRateLimiter
    final void doSetRate(double permitsPerSecond, long nowMicros) {
        resync(nowMicros);
        double stableIntervalMicros = SECONDS.toMicros(1L) / permitsPerSecond;
        this.stableIntervalMicros = stableIntervalMicros;
        doSetRate(permitsPerSecond, stableIntervalMicros);
    }
    
    //SmoothBursty
    void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
      double oldMaxPermits = this.maxPermits;
      maxPermits = maxBurstSeconds * permitsPerSecond;
      if (oldMaxPermits == Double.POSITIVE_INFINITY) {
        // if we don't special-case this, we would get storedPermits == NaN, below
        storedPermits = maxPermits;
      } else {
        storedPermits = (oldMaxPermits == 0.0)
            ? 0.0 // initial state
            : storedPermits * maxPermits / oldMaxPermits;
      }
    }
    //SmoothWarmingUp
    void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
      double oldMaxPermits = maxPermits;
      maxPermits = warmupPeriodMicros / stableIntervalMicros;       //maxPermits=等于warmup期间生成的permit
      halfPermits = maxPermits / 2.0;
      // Stable interval is x, cold is 3x, so on average it's 2x. Double the time -> halve the rate
      double coldIntervalMicros = stableIntervalMicros * 3.0;
      slope = (coldIntervalMicros - stableIntervalMicros) / halfPermits;
      if (oldMaxPermits == Double.POSITIVE_INFINITY) {
        // if we don't special-case this, we would get storedPermits == NaN, below
        storedPermits = 0.0;
      } else {
        storedPermits = (oldMaxPermits == 0.0)
            ? maxPermits // initial state is cold
            : storedPermits * maxPermits / oldMaxPermits;
      }
  }
    //SmoothWarmingUp
    void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
      double oldMaxPermits = maxPermits;
      maxPermits = warmupPeriodMicros / stableIntervalMicros;
      halfPermits = maxPermits / 2.0;
      // Stable interval is x, cold is 3x, so on average it's 2x. Double the time -> halve the rate
      double coldIntervalMicros = stableIntervalMicros * 3.0;
      slope = (coldIntervalMicros - stableIntervalMicros) / halfPermits;
      if (oldMaxPermits == Double.POSITIVE_INFINITY) {
        // if we don't special-case this, we would get storedPermits == NaN, below
        storedPermits = 0.0;
      } else {
        storedPermits = (oldMaxPermits == 0.0)
            ? maxPermits // initial state is cold
            : storedPermits * maxPermits / oldMaxPermits;
      }
    }
    
    public double acquire(int permits) {
        long microsToWait = reserve(permits);
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }
    
    final long reserve(int permits) {
        synchronized (mutex()) {
          return reserveAndGetWaitLength(permits, stopwatch.readMicros());  //reserveAndGetWaitLength(1, 从开始为止经过的微妙数)
        }
    }
    
    final long reserveAndGetWaitLength(int permits, long nowMicros) {
        long momentAvailable = reserveEarliestAvailable(permits, nowMicros);
        return max(momentAvailable - nowMicros, 0);
    }

    //返回permits可用时间
    //SmoothRateLimiter
    final long reserveEarliestAvailable(int requiredPermits, long nowMicros) {
        resync(nowMicros);
        long returnValue = nextFreeTicketMicros;
        double storedPermitsToSpend = min(requiredPermits, this.storedPermits);
        double freshPermits = requiredPermits - storedPermitsToSpend;

        long waitMicros = storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend)
            + (long) (freshPermits * stableIntervalMicros);

        this.nextFreeTicketMicros = nextFreeTicketMicros + waitMicros;
        this.storedPermits -= storedPermitsToSpend;
        return returnValue;
    }

    // 计算生成这些许可数需要等待的时间
    //SmoothBursty
    long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
      return 0L;
    }
    // 计算生成这些许可数需要等待的时间
    // 当storedPermits小于等于
    //SmoothWarmingUp
    long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
      double availablePermitsAboveHalf = storedPermits - halfPermits;       //storedPermits中超过半数的permit
      long micros = 0;
      // measuring the integral on the right part of the function (the climbing line)
      if (availablePermitsAboveHalf > 0.0) {
        double permitsAboveHalfToTake = min(availablePermitsAboveHalf, permitsToTake);  //storedPermits中超过半数，并且能够取走的permit
                //计算取走permitsAboveHalfToTake数量permit所需要的平均耗时
        micros = (long) (permitsAboveHalfToTake * (permitsToTime(availablePermitsAboveHalf)
            + permitsToTime(availablePermitsAboveHalf - permitsAboveHalfToTake)) / 2.0);
              //去除已经计算过的permitsToTake
        permitsToTake -= permitsAboveHalfToTake;
      }
      // measuring the integral on the left part of the function (the horizontal line)
      micros += (stableIntervalMicros * permitsToTake);
      return micros;
    }
    //SmoothWarmingUp
    private double permitsToTime(double permits) {
      return stableIntervalMicros + permits * slope;
    }
    
    //SmoothRateLimiter
    private void resync(long nowMicros) {
        // if nextFreeTicket is in the past, resync to now
        if (nowMicros > nextFreeTicketMicros) {
          storedPermits = min(maxPermits,
              storedPermits + (nowMicros - nextFreeTicketMicros) / stableIntervalMicros);
          nextFreeTicketMicros = nowMicros;
        }
    }
    
    SmoothWarmingUp(SleepingStopwatch stopwatch, long warmupPeriod, TimeUnit timeUnit) {
      super(stopwatch);
      this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
    }
    
    
    abstract static class SleepingStopwatch {
        
        abstract long readMicros();

        abstract void sleepMicrosUninterruptibly(long micros);

        static final SleepingStopwatch createFromSystemTimer() {
          return new SleepingStopwatch() {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            @Override
            long readMicros() {
              return stopwatch.elapsed(MICROSECONDS);
            }

            @Override
            void sleepMicrosUninterruptibly(long micros) {
              if (micros > 0) {
                Uninterruptibles.sleepUninterruptibly(micros, MICROSECONDS);
              }
            }
          };
        }
    }
}

/*
An object that measures elapsed time in nanoseconds. It is useful to measure elapsed time using this class instead of direct calls to System.nanoTime for a few reasons:
    An alternate time source can be substituted, for testing or performance reasons.
    As documented by nanoTime, the value returned has no absolute meaning, and can only be interpreted as relative to another timestamp returned by nanoTime at a different time. Stopwatch is a more effective abstraction because it exposes only these relative values, not the absolute ones.
*/
public final class Stopwatch {
    
    public static Stopwatch createStarted() {
        return new Stopwatch().start();
    }
    
    private final Ticker ticker;
    private boolean isRunning;
    private long elapsedNanos;
    private long startTick;
    
    Stopwatch() {
        this(Ticker.systemTicker()); //相当于 this.ticker = Ticker.systemTicker();
    }
    
    public Stopwatch start() {
        isRunning = true;
        startTick = ticker.read();  //相当于startTick=System.nanoTime();
        return this;
    }
    
    public long elapsed(TimeUnit desiredUnit) {
        return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
    }
    
    private long elapsedNanos() {
        return isRunning ? ticker.read() - startTick + elapsedNanos : elapsedNanos;
    }
}

public abstract class Ticker {
    public static Ticker systemTicker() {
        return SYSTEM_TICKER;
    }

    private static final Ticker SYSTEM_TICKER = new Ticker() {
        @Override
        public long read() {
          return Platform.systemNanoTime();
        }
    };
}
