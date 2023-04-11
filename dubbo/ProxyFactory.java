基于版本：2.5.7

@SPI("javassist")
interface ProxyFactory {

    @Adaptive({Constants.PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    @Adaptive({Constants.PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;

}

class JavassistProxyFactory extends AbstractProxyFactory {

    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper类不能正确处理带$的类名
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }

}

class Wrapper{

    public static Wrapper getWrapper(Class<?> c) {
        。。。
        ret = makeWrapper(c);
        。。。
        return ret;
    }

    //动态生成Wrapper类，用于包装被代理对象
    public static Wrapper makeWrapper(Class<?> c) {
        。。。
    }
}

//动态生成的Wrapper类，以包装HemaRiderProviderImpl对象为例
public class Wrapper26 extends Wrapper implements DC {
    public Object invokeMethod(Object instance, String methodName, Class[] parameterTypes, Object[] args) throws InvocationTargetException {
        HemaRiderProviderImpl var5;
        try {
            var5 = (HemaRiderProviderImpl)instance;
        } catch (Throwable var8) {
            throw new IllegalArgumentException(var8);
        }

        try {
            if("register".equals(methodName) && parameterTypes.length == 1) {
                return var5.register((RegisterReq)args[0]);
            }
            // 以下略，包装了所有的方法
        } catch (Throwable var9) {
            throw new InvocationTargetException(var9);
        }

        throw new NoSuchMethodException("Not found method \"" + var2 + "\" in class com.dianwoba.hema.provider.HemaRiderProviderImpl.");
    }
}






