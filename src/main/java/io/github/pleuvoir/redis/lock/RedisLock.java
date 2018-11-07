package io.github.pleuvoir.redis.lock;

import java.util.Arrays;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import io.github.pleuvoir.redis.kit.PropertiesWrap;
import io.github.pleuvoir.redis.kit.RedisPluginConfigUtils;

public class RedisLock implements Lock, InitializingBean {

	@Resource(name = "stringRedisTemplate")
	private StringRedisTemplate redisTemplate;

	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;

	private String lockKeyPreix;
	private RedisScript<Long> lockScript;
	private RedisScript<Long> unlockScript;

	private String generateKey(String key) {
		return lockKeyPreix.concat(key);
	}

	@Override
	public boolean lock(String key) {
		return lock(key, RedisPluginConfigUtils.DEFAULT_REDIS_LOCK_TIMEOUT);
	}
	
	@Override
	public boolean lock(String key, String timeout) {
		Long retVal = redisTemplate.execute(this.lockScript,
							Arrays.asList(generateKey(key), RedisPluginConfigUtils.LOCK_VALUE),
							timeout);

		return RedisPluginConfigUtils.REDIS_LOCK_ACQUIRE_SUCCESS.equals(retVal);
	}

	@Override
	public void unlock(String key) {
		redisTemplate.execute(this.unlockScript, Arrays.asList(generateKey(key), RedisPluginConfigUtils.LOCK_VALUE));
	}

	@Override
	public boolean isLocked(String key) {
		return StringUtils.equals(RedisPluginConfigUtils.LOCK_VALUE, redisTemplate.opsForValue().get(generateKey(key)));
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		String propCachePrefix = config.getString("redis.cacheManager.prefix");
		
		this.lockKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat(":limit") : "unkown:limit";

		this.lockScript = new DefaultRedisScript<>(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/lock.lua")).getScriptAsString(),
				Long.class);

		this.unlockScript = new DefaultRedisScript<>(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/unlock.lua")).getScriptAsString(),
				Long.class);
	}

}
