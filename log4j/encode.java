class Encoder{
	void encode(T source, ByteBufferDestination destination);
}

class PatternLayout{
	public void encode(final LogEvent event, final ByteBufferDestination destination) {
        if (!(eventSerializer instanceof Serializer2)) {
            super.encode(event, destination);
            return;
        }
        final StringBuilder text = toText((Serializer2) eventSerializer, event, getStringBuilder());
        final Encoder<StringBuilder> encoder = getStringBuilderEncoder();
        encoder.encode(text, destination);
        trimToMaxSize(text);
    }
    // 将log event写入StringBuilder
    private StringBuilder toText(final Serializer2 serializer, final LogEvent event,
            final StringBuilder destination) {
        return serializer.toSerializable(event, destination);
    }
}