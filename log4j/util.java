class ReflectionUtil{

	public static Stack<Class<?>> getCurrentStackTrace() {
        // benchmarks show that using the SecurityManager is much faster than looping through getCallerClass(int)
        if (SECURITY_MANAGER != null) {
            final Class<?>[] array = SECURITY_MANAGER.getClassContext();	//调用了SecurityManager#getClassContext()
            final Stack<Class<?>> classes = new Stack<>();
            classes.ensureCapacity(array.length);
            for (final Class<?> clazz : array) {
                classes.push(clazz);
            }
            return classes;
        }
        // slower version using getCallerClass where we cannot use a SecurityManager
        if (supportsFastReflection()) {
            final Stack<Class<?>> classes = new Stack<>();
            Class<?> clazz;
            for (int i = 1; null != (clazz = getCallerClass(i)); i++) {
                classes.push(clazz);
            }
            return classes;
        }
        return new Stack<>();
    }
}

//SecurityManager#getClassContext()栈信息：
0:类当前正在执行的方法
1:类方法的调用者