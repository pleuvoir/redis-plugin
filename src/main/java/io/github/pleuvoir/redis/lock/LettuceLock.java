package io.github.pleuvoir.redis.lock;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import io.github.pleuvoir.redis.kit.PropertiesWrap;

public class LettuceLock implements Lock, InitializingBean {

	private static final Long REDIS_LOCK_ACQUIRE_SUCCESS = 1L;

	private static final String DEFAULT_REDIS_LOCK_TIMEOUT = String.valueOf(5);

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
	public boolean lock(String key, String owner) {
		return lock(key, owner, DEFAULT_REDIS_LOCK_TIMEOUT);
	}
	
	@Override
	public boolean lock(String key, String owner, String timeout) {
		
		String keys1 = generate(key);
		String keys2 = owner;
		List<String> keys = Arrays.asList(keys1,keys2);
		
		String argv1 = timeout;
		
		Long retVal = redisTemplate.execute(this.lockScript, keys, argv1);
		
		return REDIS_LOCK_ACQUIRE_SUCCESS.equals(retVal);
	}

	@Override
	public void unlock(String key, String owner) {

		String keys1 = generate(key);
		List<String> keys = Arrays.asList(keys1);

		String argv1 = owner;

		redisTemplate.execute(this.unlockScript, keys, argv1);
	}

	@Override
	public boolean isLocked(String key) {
		return redisTemplate.hasKey(generate(key));
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
