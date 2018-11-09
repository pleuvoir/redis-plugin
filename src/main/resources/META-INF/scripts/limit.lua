
local c =  tonumber(redis.call('get', KEYS[1]) or "0")
local limitCount = tonumber(ARGV[1])
local limitPeriod = ARGV[2]
if c > limitCount then
return c;
end
c = redis.call('incr',KEYS[1])
if c == 1 then
redis.call('expire',KEYS[1],limitPeriod)
end
return c;