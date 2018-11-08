package io.github.pleuvoir.redis.limit;

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

public class LettuceRedisRateLimit implements RateLimit, InitializingBean {

	@Resource(name = "stringRedisTemplate")
	private StringRedisTemplate redisTemplate;
	
	private String limitKeyPreix;
	private RedisScript<Long> limitScript;
	
	@Resource(name = "redisPropertiesWrap")
	private PropertiesWrap config;
	
	private String generate(String key) {
		return limitKeyPreix.concat(key);
	}
	
	@Override
	public boolean tryAccess(String name, String key, int limitPeriod, int limitCount) {
		
		String keys1 = generate(name.concat(key));
		List<String> keys = Arrays.asList("aquarius_limit_X-Y");
		
		String argv1 = String.valueOf(limitPeriod);
		String argv2 = String.valueOf(limitCount);
		
		Long count = redisTemplate.execute(limitScript, keys, argv1, argv2);
		System.out.println(count);
	//	return count.intValue() <= limitCount;
		return true;
	}
	
	private String buildLuaScript() {
	        StringBuilder lua = new StringBuilder();
	        lua.append("local c");
	        lua.append("\nc = redis.call('get',KEYS[1])");
	        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then"); // 调用不超过最大值，则直接返回
	        lua.append("\nreturn c;");
	        lua.append("\nend");
	        lua.append("\nc = redis.call('incr',KEYS[1])"); // 执行计算器自加
	        lua.append("\nif tonumber(c) == 1 then");
	        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])"); // 从第一次调用开始限流，设置对应键值的过期
	        lua.append("\nend");
	        lua.append("\nreturn c;");
	        return lua.toString();
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		String propCachePrefix = config.getString("redis.cacheManager.prefix");
		
		this.limitKeyPreix = StringUtils.isNotBlank(propCachePrefix) ? propCachePrefix.concat("{slot:limit}") : "{unkown:limit}";
		
		this.limitScript = new DefaultRedisScript<>(buildLuaScript(), Long.class);
		
//		this.limitScript = new DefaultRedisScript<>(
//				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/limit.lua")).getScriptAsString(),
//				Long.class);
	}

}
