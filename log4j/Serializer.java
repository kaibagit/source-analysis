// PatternLayout内部类
class PatternSerializer{

	public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
        final int len = formatters.length;
        for (int i = 0; i < len; i++) {
            formatters[i].format(event, buffer);
        }
        if (replace != null) { // creates temporary objects
            String str = buffer.toString();
            str = replace.format(str);
            buffer.setLength(0);
            buffer.append(str);
        }
        return buffer;
    }
}

class PatternFormatter{
	public void format(final LogEvent event, final StringBuilder buf) {
        if (skipFormattingInfo) {
            converter.format(event, buf);
        } else {
            formatWithInfo(event, buf);
        }
    }
}

ExtendedThrowablePatternConverter << ThrowablePatternConverter << LogEventPatternConverter << AbstractPatternConverter

class ExtendedThrowablePatternConverter{

	public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final ThrowableProxy proxy = event.getThrownProxy();
        final Throwable throwable = event.getThrown();
        if ((throwable != null || proxy != null) && options.anyLines()) {
            if (proxy == null) {
                super.format(event, toAppendTo);
                return;
            }
            final String extStackTrace = proxy.getExtendedStackTraceAsString(options.getIgnorePackages(), options.getTextRenderer());
            final int len = toAppendTo.length();
            if (len > 0 && !Character.isWhitespace(toAppendTo.charAt(len - 1))) {
                toAppendTo.append(' ');
            }
            if (!options.allLines() || !Strings.LINE_SEPARATOR.equals(options.getSeparator())) {
                final StringBuilder sb = new StringBuilder();
                final String[] array = extStackTrace.split(Strings.LINE_SEPARATOR);
                final int limit = options.minLines(array.length) - 1;
                for (int i = 0; i <= limit; ++i) {
                    sb.append(array[i]);
                    if (i < limit) {
                        sb.append(options.getSeparator());
                    }
                }
                toAppendTo.append(sb.toString());
            } else {
                toAppendTo.append(extStackTrace);
            }
        }
    }
}

//Wraps a Throwable to add packaging information about each stack trace element.
//A proxy is used to represent a throwable that may not exist in a different class loader or JVM. 
//When an application deserializes a ThrowableProxy, the throwable may not be set, but the throwable's information is preserved in other fields of the proxy like the message and stack trace.
class ThrowableProxy{

	// Cached StackTracePackageElement and ClassLoader.
	static class CacheEntry {
        private final ExtendedClassInfo element;
        private final ClassLoader loader;

        public CacheEntry(final ExtendedClassInfo element, final ClassLoader loader) {
            this.element = element;
            this.loader = loader;
        }
    }

	// Constructs the wrapper for the Throwable that includes packaging data.
	public ThrowableProxy(final Throwable throwable) {
        this(throwable, null);
    }

    // Constructs the wrapper for the Throwable that includes packaging data.
    private ThrowableProxy(final Throwable throwable, final Set<Throwable> visited) {
        this.throwable = throwable;
        this.name = throwable.getClass().getName();
        this.message = throwable.getMessage();
        this.localizedMessage = throwable.getLocalizedMessage();
        final Map<String, CacheEntry> map = new HashMap<>();
        final Stack<Class<?>> stack = ReflectionUtil.getCurrentStackTrace();	//调用栈
        this.extendedStackTrace = this.toExtendedStackTrace(stack, map, null, throwable.getStackTrace());
        final Throwable throwableCause = throwable.getCause();
        final Set<Throwable> causeVisited = new HashSet<>(1);
        this.causeProxy = throwableCause == null ? null : new ThrowableProxy(throwable, stack, map, throwableCause,
            visited, causeVisited);
        this.suppressedProxies = this.toSuppressedProxies(throwable, visited);
    }

    // 解析 all the stack entries in this stack trace that are not common with the parent.
    // stack : The callers Class stack.
    // map : The cache of CacheEntry objects.
    // rootTrace  : The first stack trace resolve or null.
    // stackTrace : The stack trace being resolved.
    ExtendedStackTraceElement[] toExtendedStackTrace(final Stack<Class<?>> stack, final Map<String, CacheEntry> map,
                                                     final StackTraceElement[] rootTrace{default:null},
                                                     final StackTraceElement[] stackTrace) {
    	int stackLength;
    	if (rootTrace != null) {
            。。。
        } else {
            this.commonElementCount = 0;
            stackLength = stackTrace.length;
        }
        final ExtendedStackTraceElement[] extStackTrace = new ExtendedStackTraceElement[stackLength];
        Class<?> clazz = stack.isEmpty() ? null : stack.peek();	//取出栈顶元素，也就是最初的caller
        ClassLoader lastLoader = null;
        // 从栈底，也就是最初的caller开始遍历异常栈
        for (int i = stackLength - 1; i >= 0; --i) {
            final StackTraceElement stackTraceElement = stackTrace[i];  //当前的异常栈element
            final String className = stackTraceElement.getClassName();  //异常栈中的类名
            // The stack returned from getCurrentStack may be missing entries for java.lang.reflect.Method.invoke()
            // and its implementation. The Throwable might also contain stack entries that are no longer
            // present as those methods have returned.
            ExtendedClassInfo extClassInfo;
            if (clazz != null && className.equals(clazz.getName())) {
                final CacheEntry entry = this.toCacheEntry(stackTraceElement, clazz, true);
                extClassInfo = entry.element;
                lastLoader = entry.loader;
                stack.pop();
                clazz = stack.isEmpty() ? null : stack.peek();
            } else {    //调用栈与异常栈的类名不一致
                final CacheEntry cacheEntry = map.get(className);
                if (cacheEntry != null) {
                    final CacheEntry entry = cacheEntry;
                    extClassInfo = entry.element;
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                } else {
                    final CacheEntry entry = this.toCacheEntry(stackTraceElement,
                        this.loadClass(lastLoader, className), false);
                    extClassInfo = entry.element;
                    map.put(stackTraceElement.toString(), entry);   //bug
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                }
            }
            // 调用栈的元素相当于异常栈的子集
            extStackTrace[i] = new ExtendedStackTraceElement(stackTraceElement, extClassInfo);
        }
        return extStackTrace;
    }

    // exact : True if the class was obtained via Reflection.getCallerClass
    private CacheEntry toCacheEntry(final StackTraceElement stackTraceElement, final Class<?> callerClass,
                                    final boolean exact{default:true}) {
        String location = "?";
        String version = "?";
        ClassLoader lastLoader = null;
        if (callerClass != null) {
            try {
                final CodeSource source = callerClass.getProtectionDomain().getCodeSource();
                if (source != null) {
                    final URL locationURL = source.getLocation();
                    if (locationURL != null) {
                        final String str = locationURL.toString().replace('\\', '/');
                        int index = str.lastIndexOf("/");
                        if (index >= 0 && index == str.length() - 1) {
                            index = str.lastIndexOf("/", index - 1);
                            location = str.substring(index + 1);
                        } else {
                            location = str.substring(index + 1);
                        }
                    }
                }
            } catch (final Exception ex) {
                // Ignore the exception.
            }
            final Package pkg = callerClass.getPackage();
            if (pkg != null) {
                final String ver = pkg.getImplementationVersion();
                if (ver != null) {
                    version = ver;
                }
            }
            lastLoader = callerClass.getClassLoader();
        }
        return new CacheEntry(new ExtendedClassInfo(exact, location, version), lastLoader);
    }
}

// 封装StackTraceElement对象，并增加拓展信息
class ExtendedStackTraceElement{
	private final ExtendedClassInfo extraClassInfo;

    private final StackTraceElement stackTraceElement;
}
// Class and package data used with a StackTraceElement in a ExtendedStackTraceElement.
class ExtendedClassInfo{

    private final boolean exact;

    private final String location;

    private final String version;

}


