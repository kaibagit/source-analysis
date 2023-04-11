class MetricRegistry{

	public Meter meter(String name) {
        return getOrAdd(name, MetricBuilder.METERS);
    }

    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder) {
    	final Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            ..
            return register(name, builder.newMetric());
            ..
        }
    	..
    }
}