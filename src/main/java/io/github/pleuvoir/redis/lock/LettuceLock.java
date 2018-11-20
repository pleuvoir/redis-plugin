package io.github.pleuvoir.redis.lock;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import io.github.pleuvoir.base.kit.PropertiesWrap;

public class LettuceLock implements Lock, InitializingBean {

	private static final Long LOCK_OR_UNLOCK_SUCCESS = 1L;

	private static final String DEFAULT_REDIS_LOCK_TIMEOUT = String.valueOf(5);

	private ThreadLocal<String> RANDOM_VALUE = new ThreadLocal<>();
	
	@Resource(name = "stringRedisTemplate")
	private StringRedisTemplate redisTemplate;

	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;

	private String lockKeyPreix;
	private RedisScript<Long> lockScript;
	private RedisScript<Long> unlockScript;

	private String generate(String key) {
		return lockKeyPreix.concat(key);
	}

	@Override
	public boolean lock(String key) {
		return lock(key, DEFAULT_REDIS_LOCK_TIMEOUT);
	}
	
	@Override
	public boolean lock(String key, String timeout) {
		
		String keys1 = generate(key);
		String keys2 = generate(fastUUID().toString());
		
		List<String> keys = Arrays.asList(keys1,keys2);
		
		String argv1 = timeout;
		
		Long retVal = redisTemplate.execute(this.lockScript, keys, argv1);
		
		boolean lockStatus = LOCK_OR_UNLOCK_SUCCESS.equals(retVal);
		// 只有加锁成功时，才设置随机值，方便解锁时使用
		if (lockStatus) {
			RANDOM_VALUE.set(keys2);
		}
		return lockStatus;
	}

	@Override
	public void unlock(String key) {

		String keys1 = generate(key);
		List<String> keys = Arrays.asList(keys1);

		String argv1 = RANDOM_VALUE.get();
		
		Long retVal = redisTemplate.execute(this.unlockScript, keys, argv1);
		
		boolean unlockStatus = LOCK_OR_UNLOCK_SUCCESS.equals(retVal);
		// 只有解锁成功时，才移除随机值
		if (unlockStatus) {
			RANDOM_VALUE.remove();
		}
	}

	@Override
	public boolean isLocked(String key) {
		return redisTemplate.hasKey(generate(key));
	}

	/*
	 * 返回使用ThreadLocalRandm的UUID，比默认的UUID性能更优
	 */
	public UUID fastUUID() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		return new UUID(random.nextLong(), random.nextLong());
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		String propCachePrefix = config.getString("redis.cacheManager.prefix");
		
		this.lockKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat("{lock}") : "{unkown:lock}";

		this.lockScript = new DefaultRedisScript<>(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/lock.lua")).getScriptAsString(),
				Long.class);

		this.unlockScript = new DefaultRedisScript<>(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/unlock.lua")).getScriptAsString(),
				Long.class);
	}

}
