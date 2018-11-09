
local key = KEYS[1] 
local content = KEYS[2] 
local ttl = ARGV[1] 
local current = redis.call('setnx', key, content)
if current == 1 then
	redis.call('expire', key, ttl)
return current
end