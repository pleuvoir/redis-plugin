package io.github.pleuvoir.redis.limit;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import io.github.pleuvoir.redis.kit.PropertiesWrap;

public class LettuceRedisRateLimit implements RateLimit, InitializingBean {

	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;
	
	private String limitKeyPreix;
	private RedisScript<Number> limitScript;
	
	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;
	
	private String generate(String key) {
		return limitKeyPreix.concat(key);
	}
	
	@Override
	public boolean tryAccess(String name, String key, int limitPeriod, int limitCount) {
		
		String keys1 = generate(name.concat(key));
		List<String> keys = Arrays.asList(keys1);
		
		Integer argv1 = limitCount;
		Integer argv2 = limitPeriod;
		
		Number count = redisTemplate.execute(limitScript, keys, argv1, argv2);
		return count.intValue() <= limitCount;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		String propCachePrefix = config.getString("redis.cacheManager.prefix");
		
		this.limitKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat("{slot}") : "{unkown}";
		
		this.limitScript = new DefaultRedisScript<>(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/limit.lua")).getScriptAsString(),
				Number.class);
	}

}
