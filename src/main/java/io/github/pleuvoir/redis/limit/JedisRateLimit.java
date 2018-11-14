package io.github.pleuvoir.redis.limit;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import io.github.pleuvoir.redis.kit.PropertiesWrap;
import io.github.pleuvoir.redis.lock.Lock;

public class JedisRateLimit implements RateLimit, InitializingBean {

	private String limitKeyPreix;
	
	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;
	
	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;
	
	@Autowired
	private Lock lock;

	@Override
	public boolean tryAccess(String name, String key, int limitPeriod, int limitCount) {

		ValueOperations<String, Object> ops = opsForValue();
		String keys = generate(name.concat(key));
		try {
			lock.lock(keys);
			Long newValue = ops.increment(keys, 1);
			if (newValue == 1L) {
				redisTemplate.expire(keys, limitPeriod, TimeUnit.SECONDS);
			}
			return newValue <= limitCount;
		} finally {
			lock.unlock(keys);
		}
	}

	private ValueOperations<String, Object> opsForValue() {
		return redisTemplate.opsForValue();
	}

	private String generate(String key) {
		return limitKeyPreix.concat(key);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.limitKeyPreix = config.getString("redis.cacheManager.prefix");
	}
}
