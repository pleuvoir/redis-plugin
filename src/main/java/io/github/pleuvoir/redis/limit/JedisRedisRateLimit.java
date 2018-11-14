package io.github.pleuvoir.redis.limit;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import io.github.pleuvoir.redis.kit.PropertiesWrap;

public class JedisRedisRateLimit implements RateLimit, InitializingBean {

	private String limitKeyPreix;

	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;

	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;


	@Override
	public boolean tryAccess(String name, String key, int limitPeriod, int limitCount) {

		String keys = generate(name.concat(key));
		ValueOperations<String, Object> ops = opsForValue();

		Boolean hasKey = redisTemplate.hasKey(keys);
		if (!hasKey) {
			ops.set(keys, limitCount);
			redisTemplate.expire(key, limitPeriod, TimeUnit.SECONDS);
			return true;
		}
		
		Integer oldValue = (Integer) ops.get(keys);
		Long increment = ops.increment(keys, oldValue);
		// not first
		return increment <= limitCount;
	}


	private ValueOperations<String, Object> opsForValue() {
		return redisTemplate.opsForValue();
	}

	private String generate(String key) {
		return limitKeyPreix.concat(key);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		String propCachePrefix = config.getString("redis.cacheManager.prefix");

		this.limitKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat("{slot}") : "{unkown}";
	}
}
