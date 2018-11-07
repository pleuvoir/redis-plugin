package io.github.pleuvoir.redis.lock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import io.github.pleuvoir.redis.kit.PropertiesWrap;

public class RedisLock implements Lock {

	@Autowired
	@Qualifier("redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	@Qualifier("redisPropertiesWrap")
	private PropertiesWrap config;

	private static final String LOCK_VALUE = "locks";
	
	private String lockScript;
	private String unlockScript;

	@PostConstruct
	public void initialize() throws IOException {
		this.lockScript = new ResourceScriptSource(new ClassPathResource("META-INF/scripts/lock.lua")).getScriptAsString();
		this.unlockScript = new ResourceScriptSource(new ClassPathResource("META-INF/scripts/unlock.lua")).getScriptAsString();
	}

	private String generateKey(String key) {
		return config.getString("redis.cacheManager.prefix", "redis-plugin:lock").concat(key);
	}


	@Override
	public boolean lock(String key) {
//		String lockKey = key; 
//		RedisScript<Number> redisScript = new DefaultRedisScript<>(lockScript, Number.class);
//		List<String> keyList = new ArrayList<String>();
//		keyList.add(lockKey); 
//		keyList.add("22sewfge4w"); 
//		Number execute = redisTemplate.execute(redisScript, keyList, "10000");
//		System.out.println(execute);
	    String luaScript = buildLuaScript();
	    List<String> keyList = new ArrayList<String>();
	    keyList.add("redis-plugin:lock-11111");
        RedisScript<Integer> redisScript = new DefaultRedisScript<>(luaScript, Integer.class);
        Integer count = redisTemplate.execute(redisScript, keyList, 10, 15);
        System.out.println(count);
		return false;
	}
	
	/**
     * 限流 脚本
     *
     * @return lua脚本
     */
    public String buildLuaScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("local c");
        lua.append("\nc = redis.call('get',KEYS[1])");
        // 调用不超过最大值，则直接返回
        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");
        lua.append("\nreturn c;");
        lua.append("\nend");
        // 执行计算器自加
        lua.append("\nc = redis.call('incr',KEYS[1])");
        lua.append("\nif tonumber(c) == 1 then");
        // 从第一次调用开始限流，设置对应键值的过期
        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");
        lua.append("\nend");
        lua.append("\nreturn c;");
        return lua.toString();
    }
    
	@Override
	public void unlock(String key) {
		RedisScript<Number> redisScript = new DefaultRedisScript<>(unlockScript, Number.class);
		Number execute = redisTemplate.execute(redisScript, Arrays.asList(generateKey(key)), LOCK_VALUE);
		System.out.println(execute);
	}

	@Override
	public boolean isLocked(String key) {
		return false;
	}

}
