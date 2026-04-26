local key = KEYS[1]
local time_window = tonumber(ARGV[1])
local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, time_window)
end
return current
