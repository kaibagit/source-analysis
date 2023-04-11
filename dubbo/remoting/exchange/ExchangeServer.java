
interface ExchangeServer extends Server{

}

class HeaderExchangeServer implements ExchangeServer{

    private final Server server;    //被包装的Server，比如NettyServer
    // 心跳定时器
    private ScheduledFuture<?> heatbeatTimer;
    // 心跳超时，毫秒。缺省0，不会执行心跳。
    private int heartbeat;
    private int heartbeatTimeout;

    public HeaderExchangeServer(Server server) {
        。。。
        this.server = server;
        this.heartbeat = server.getUrl().getParameter(Constants.HEARTBEAT_KEY, 0);
        this.heartbeatTimeout = server.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
        。。。
        startHeatbeatTimer();
    }
}