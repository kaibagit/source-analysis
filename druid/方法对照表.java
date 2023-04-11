
获取连接：
DruidDataSource#getConnection(maxWaitMillis)
-> FilterChainImpl#dataSource_connect(dataSource,maxWaitMillis)
	-> Filter#dataSource_getConnection(chain,dataSource,maxWaitMillis)
		-> DruidDataSource#getConnectionDirect(maxWaitMillis) => DruidPooledConnection
			-> DruidDataSource#getConnectionInternal(maxWait) => DruidPooledConnection
				|-> DruidDataSource#createPhysicalConnection() => PhysicalConnectionInfo


建立真实的连接：
DruidDataSource#createPhysicalConnection()
-> DruidDataSource#createPhysicalConnection(url,info) => ConnectionProxyImpl
	-> FilterChainImpl#connection_connect(info) => ConnectionProxyImpl
		-> Filter#connection_connect(chain,info)
			-> Drive#connect(url,info) => Connection

PreparedStatement创建：
DruidPooledConnection#prepareStatement(sql) => DruidPooledPreparedStatement
-> ConnectionProxyImpl#prepareStatement(sql)
	-> FilterChainImpl#connection_prepareStatement(connection,sql) => PreparedStatementProxyImpl
		-> Filter#connection_prepareStatement(chain,connection,sql)
			-> Jdbc驱动的Connection#prepareStatement(sql)


Connection封装层次关系：
DruidPooledConnection{
	DruidConnectionHolder{
		ConnectionProxyImpl{
			Jdbc驱动的Connection
		}
	}
}


DruidPooledPreparedStatement{
	PreparedStatementHolder{
		PreparedStatementProxyImpl{
			Jdbc驱动的PreparedStatement
		}
	}
}


