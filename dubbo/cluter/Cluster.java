@SPI(FailoverCluster.NAME)
interface Cluster{

	// 主要功能是，根据具体某个目录，生成一个虚拟的Invoker，里面可能包含一个或多个真实的Invoker
	// 返回的Invoker有FailoverClusterInvoker、FailfastClusterInvoker等
	@Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;
}


public class FailoverCluster implements Cluster {

    public final static String NAME = "failover";

    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailoverClusterInvoker<T>(directory);
    }

}
