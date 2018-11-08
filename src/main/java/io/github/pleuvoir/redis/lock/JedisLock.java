package io.github.pleuvoir.redis.lock;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import io.github.pleuvoir.redis.kit.PropertiesWrap;
import io.github.pleuvoir.redis.kit.RedisPluginConfigUtils;

public class JedisLock implements Lock, InitializingBean {

	@Resource(name = "stringRedisTemplate")
	private StringRedisTemplate redisTemplate;

	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;

	private String lockKeyPreix;

	private String generate(String key) {
		return lockKeyPreix.concat(key);
	}

	private ValueOperations<String, String> forValue() {
		return redisTemplate.opsForValue();
	}

	@Override
	public boolean lock(String key) {
		return lock(key, RedisPluginConfigUtils.DEFAULT_REDIS_LOCK_TIMEOUT);
	}

	@Override
	public boolean lock(String key, String timeout) {
		String lockKey = generate(key);
		ValueOperations<String, String> ops = forValue();
		Boolean isDone = ops.setIfAbsent(lockKey, RedisPluginConfigUtils.LOCK_VALUE);
		if (isDone == null || !isDone) {
			return false;
		}
		redisTemplate.expire(lockKey, Long.valueOf(timeout), TimeUnit.SECONDS);
		return true;
	}

	@Override
	public void unlock(String key) {
		redisTemplate.delete(generate(key));
	}

	@Override
	public boolean isLocked(String key) {
		return redisTemplate.hasKey(generate(key));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String propCachePrefix = config.getString("redis.cacheManager.prefix");
		this.lockKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat(":lock") : "unkown:lock";
	}

}