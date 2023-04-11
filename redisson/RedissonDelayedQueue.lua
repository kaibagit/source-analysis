插入待执行任务：

keys:
    getName() -> 用于存放就绪元素的RBlockingQueue的list，该list的名字
    getTimeoutSetName() -> 还未到期元素的zset，zset的key名字，如getName()为"myqueue"，则为"redisson_delay_queue_timeout:{myqueue}"
    getQueueName() -> 延迟队列，存放带延时信息的元素结构体，如getName()为"myqueue"，则为"redisson_delay_queue:{myqueue}"
params:
    getChannelName() -> 如getName()为"myqueue"，则为"redisson_delay_queue_channel:{myqueue}"

params:
    timeout -> 任务的到期时间戳
    randomId -> long类型随机数
    e -> 待就绪的元素

-- 1、将元素e封装一层
-- value = [randomId,e的长度,e]
local value = struct.pack('dLc0', tonumber(ARGV[2]), string.len(ARGV[3]), ARGV[3]);

-- 2、将任务加入到zset
-- 翻译：zadd getTimeoutSetName() timeout value
redis.call('zadd', KEYS[2], ARGV[1], value);

-- 3、将任务加到延迟队列
-- 翻译：rpush getQueueName() value
redis.call('rpush', KEYS[3], value);

-- 4、从timeout zset中，取出第一个元素
-- 翻译：zrange getTimeoutSetName() 0 0
local v = redis.call('zrange', KEYS[2], 0, 0); 
if v[1] == value then
    -- 5、如果timeout zset第一个元素就是刚塞进去的，则发布消息通知
    -- 翻译： publish getChannelName() timeout
    redis.call('publish', KEYS[4], ARGV[1]); 
end;










查询待就绪的任务：

keys:
    getName() -> 用于存放就绪元素的RBlockingQueue的list，该list的名字
    getTimeoutSetName() -> 还未到期元素的zset，zset的key名字，如getName()为"myqueue"，则为"redisson_delay_queue_timeout:{myqueue}"
    getQueueName() -> 延迟队列，存放带延时信息的元素结构体，如getName()为"myqueue"，则为"redisson_delay_queue:{myqueue}"

params:
    System.currentTimeMillis() -> zrangebyscore的max参数
    100 -> zrangebyscore的count参数，即最多从zset获取就绪的任务数

-- 1、取出所有就绪的任务
-- 翻译：expiredValues = zrangebyscore getTimeoutSetName() 0 System.currentTimeMillis() limit 0 100
local expiredValues = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); 
if #expiredValues > 0 then 
    for i, v in ipairs(expiredValues) do 
        -- 其中，v = [randomId,e的长度,e]
        local randomId, value = struct.unpack('dLc0', v);
        -- 2、将就绪任务push到就绪的list
        -- 翻译：rpush getName() value
        redis.call('rpush', KEYS[1], value);
        -- 3、将带延时信息的元素从延迟队列中移除
        -- 翻译：lrem getQueueName() 1 v
        redis.call('lrem', KEYS[3], 1, v);
    end; 
    -- 4、将就绪的所有任务从zset中移除
    -- 翻译：zrem getTimeoutSetName() 
    redis.call('zrem', KEYS[2], unpack(expiredValues));
end; 
-- 5、查看待就绪任务zset的第一个元素
-- 翻译：zrange getTimeoutSetName() 0 0 WITHSCORES
local v = redis.call('zrange', KEYS[2], 0, 0, 'WITHSCORES'); 
-- 6、如果存在待就绪任务，则返回它的到期时间戳
if v[1] ~= nil then 
    return v[2]; 
end 
return nil





-- 核心流程：
-- 1、将延迟任务放入zset，其中score为任务到期的时间戳
-- 2、后台线程查询zset，把到期的任务，放入BlockingQueue




