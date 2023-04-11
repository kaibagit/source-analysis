class DruidPooledConnection{

	public PreparedStatement prepareStatement(String sql){
        checkState();

        PreparedStatementHolder stmtHolder = null;
        PreparedStatementKey key = new PreparedStatementKey(sql, getCatalog(), MethodType.M1);

        boolean poolPreparedStatements = holder.isPoolPreparedStatements();     //默认为false
        if (poolPreparedStatements) {
            stmtHolder = holder.getStatementPool().get(key);
        }

        if (stmtHolder == null) {
            try {
                stmtHolder = new PreparedStatementHolder(key, conn.prepareStatement(sql));	//conn为ConnectionProxyImpl实例
                holder.getDataSource().incrementPreparedStatementCount();
            } catch (SQLException ex) {
                handleException(ex, sql);
            }
        }

        initStatement(stmtHolder);

        DruidPooledPreparedStatement rtnVal = new DruidPooledPreparedStatement(this, stmtHolder);

        holder.addTrace(rtnVal);

        return rtnVal;
    }

}

class ConnectionProxyImpl{

	public PreparedStatement prepareStatement(String sql) throws SQLException {
        FilterChainImpl chain = createChain();
        PreparedStatement stmt = chain.connection_prepareStatement(this, sql);
        recycleFilterChain(chain);
        return stmt;
    }

}

class FilterChainImpl{

	// 创建PrepareStatement
	public PreparedStatementProxy connection_prepareStatement(
            ConnectionProxy connection,
            String sql) throws SQLException
    {
        if (this.pos < filterSize) {
            return nextFilter()
                    .connection_prepareStatement(this, connection, sql);
        }

        PreparedStatement statement = connection.getRawObject()
                .prepareStatement(sql);

        if (statement == null) {
            return null;
        }

        return new PreparedStatementProxyImpl(connection, statement, sql, dataSource.createStatementId());
    }

    // PreparedStatement执行
    public boolean preparedStatement_execute(PreparedStatementProxy statement) throws SQLException {
        if (this.pos < filterSize) {
            return nextFilter().preparedStatement_execute(this, statement);
        }
        return statement.getRawObject().execute();
    }

}



class DruidPooledPreparedStatement{
	
	public DruidPooledPreparedStatement(DruidPooledConnection conn, PreparedStatementHolder holder) throws SQLException{
		。。。
		this.stmt = holder.statement;	//stmt为PreparedStatementProxyImpl实例
		。。。
	}

	public boolean execute() throws SQLException {
		。。。
		conn.beforeExecute();
        try {
            return stmt.execute();
        } catch (Throwable t) {
            。。。
        } finally {
            conn.afterExecute();
        }
	}

}

class PreparedStatementProxyImpl{

	public boolean execute() throws SQLException {
        。。。

        firstResultSet = createChain().preparedStatement_execute(this);
        return firstResultSet;
    }

}





