package io.github.pleuvoir.redis.limit;

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

public class LettuceRedisRateLimit implements RateLimit, InitializingBean {

	@Resource(name = "stringRedisTemplate")
	private StringRedisTemplate redisTemplate;
	
	private String limitKeyPreix;
	private RedisScript<Long> limitScript;
	
	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;
	
	private String generateKey(String key) {
		return limitKeyPreix.concat(key);
	}
	
	@Override
	public boolean acquire(String key, int maxTimes) {
		return RedisPluginConfigUtils.REDIS_LIMIT_ACQUIRE_SUCCESS.equals(
				redisTemplate.execute(this.limitScript, 
						Arrays.asList(generateKey(key)), 
						String.valueOf(maxTimes)));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
		String propCachePrefix = config.getString("redis.cacheManager.prefix");
		
		this.limitKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat("{slot:limit}") : "{unkown:limit}";
		
		this.limitScript = new DefaultRedisScript<>(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/limit.lua")).getScriptAsString(),
				Long.class);
	}

}
