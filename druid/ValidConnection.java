// Connection可用检查

interface ValidConnectionChecker{
	boolean isValidConnection(Connection c, String query, int validationQueryTimeout) throws Exception;
}

MySqlValidConnectionChecker{
	boolean isValidConnection(Connection c, String query, int validationQueryTimeout) throws Exception{
		//先执行ping
		//再执行query sql
	}
}