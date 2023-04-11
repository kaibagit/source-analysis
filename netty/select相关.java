// select 策略
// Provides the ability to control the behavior of the select loop.
// 如果有events需要立即处理，则a blocking select operation can be 延迟或者完全跳过
class SelectStrategy{
	// 继续执行blocking select
	int SELECT = -1;
	// 
	int CONTINUE = -2;
 
	// if the next step should be blocking select，则返回 SELECT
	// if the next step should be to not select，如果则返回 CONTINUE
	// Any value >= 0 表明 work needs to be done.
	int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;
}

class DefaultSelectStrategy implements SelectStrategy{
	
	public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT;
    }
}