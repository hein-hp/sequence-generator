-- 存储标识位的 hashKey
local hashKey = 'snowflake_worker_id_key'
-- 存储标识位的 field
local dataCenterIdKey = 'data_center_id'
-- 存储标识位的 field
local workerIdKey = 'worker_id'

-- 是否存在 snowflake_work_id_key
-- 不存在则初始化 data_center_id 和 worker_id 为 0 并返回
if (redis.call('EXISTS', hashKey) == 0) then
    redis.call('HINCRBY', hashKey, dataCenterIdKey, 0)
    redis.call('HINCRBY', hashKey, workerIdKey, 0)
    return { 0, 0 }
end

-- 若存在 snowflake_work_id_key 则获取当前的 data_center_id 和 worker_id 并转换为数字类型
local dataCenterId = tonumber(redis.call('HGET', hashKey, dataCenterIdKey))
local workerId = tonumber(redis.call('HGET', hashKey, workerIdKey))

-- 定义 dataCenterId 和 workerId 的最大值为 31
local max = 31
local resultWorkerId = 0
local resultDataCenterId = 0

-- dataCenterId 和 workerId 都达到上限则重新回归为 0
if (dataCenterId == max and workerId == max) then
    redis.call('HSET', hashKey, dataCenterIdKey, '0')
    redis.call('HSET', hashKey, workerIdKey, '0')
    -- workerId 还没有到最大值则 workerId + 1
elseif (workerId ~= max) then
    resultWorkerId = redis.call('HINCRBY', hashKey, workerIdKey, 1)
    resultDataCenterId = dataCenterId
    -- dataCenterId 还没有到最大值则 dataCenterId + 1，workId 置 0
elseif (dataCenterId ~= max) then
    resultWorkerId = 0
    resultDataCenterId = redis.call('HINCRBY', hashKey, dataCenterIdKey, 1)
    redis.call('HSET', hashKey, workerIdKey, '0')
end

return { resultWorkerId, resultDataCenterId }