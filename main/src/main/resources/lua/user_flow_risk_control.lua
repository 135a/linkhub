-- 这是一个Redis Lua脚本，用于实现简单的滑动窗口计数器功能
local key = KEYS[1]          -- 获取输入的键名，用于作为Redis中的计数器键
local time_window = tonumber(ARGV[1])  -- 获取输入的时间窗口参数，并转换为数字类型
local current = redis.call('INCR', key)  -- 对指定键执行递增操作，获取当前计数值
if current == 1 then         -- 判断当前计数值是否为1（即键是否刚被创建）
    redis.call('EXPIRE', key, time_window)  -- 如果是第一次创建，则设置键的过期时间
end
return current               -- 返回当前的计数值
