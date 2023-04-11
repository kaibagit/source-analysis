// 代表Directory Service或者Name Service
public interface Directory<T> extends Node {

    Class<T> getInterface();

    List<Invoker<T>> list(Invocation invocation) throws RpcException;
}

class RegistryDirectory<T> extends AbstractDirectory<T>{
    ..
}

class StaticDirectory<T> extends AbstractDirectory<T> {
    ..
}