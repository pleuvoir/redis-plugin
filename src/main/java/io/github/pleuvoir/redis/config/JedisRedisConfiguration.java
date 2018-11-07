package io.github.pleuvoir.redis.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;

import io.github.pleuvoir.redis.cache.CacheService;
import io.github.pleuvoir.redis.cache.RedisCacheService;
import io.github.pleuvoir.redis.kit.PropertiesWrap;
import io.github.pleuvoir.redis.limit.RedisRateLimit;
import io.github.pleuvoir.redis.lock.RedisLock;
import redis.clients.jedis.JedisPoolConfig;

@EnableCaching
@Import({ RedisLock.class, RedisRateLimit.class })
public class JedisRedisConfiguration {
	
	private String location;
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	@Bean(name = "redisPropertiesWrap")
	public PropertiesWrap redisPropertiesWrap() throws IOException {
		Assert.hasText(location, "Redis configuration file failed, no location set.");
		Properties pro = PropertiesLoaderUtils.loadProperties(new ClassPathResource(location));
		return new PropertiesWrap(pro);
	}

	@Bean(name = "jedisConnectionFactory")
	public JedisConnectionFactory getJedisConnectionFactory(@Qualifier("redisPropertiesWrap") PropertiesWrap config)
			throws IOException {
		
		JedisPoolConfig pool = new JedisPoolConfig();
		pool.setMaxTotal(config.getInteger("redis.pool.maxTotal", JedisPoolConfig.DEFAULT_MAX_TOTAL));
		pool.setMaxIdle(config.getInteger("redis.pool.maxIdle", JedisPoolConfig.DEFAULT_MAX_IDLE));
		pool.setMaxWaitMillis(config.getLong("redis.pool.maxWait", JedisPoolConfig.DEFAULT_MAX_WAIT_MILLIS));
		pool.setTestOnBorrow(config.getBoolean("redis.pool.testOnBorrow", JedisPoolConfig.DEFAULT_TEST_ON_BORROW));

		String hostAndPortStr = config.getString("redis.hostAndPort");
		Assert.hasText(hostAndPortStr, "Redis configuration failed to get [hostAndPortStr]. Please check.");

		String[] nodes = StringUtils.split(hostAndPortStr, ",");

		JedisConnectionFactory factory = null;
		if (nodes.length == 1) {
			// standalone mode
			String[] hostAndPort = StringUtils.split(nodes[0], ":");
			String host = StringUtils.trim(hostAndPort[0]);
			String port = StringUtils.trim(hostAndPort[1]);
			RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, Integer.parseInt(port));
			standaloneConfig.setDatabase(config.getInteger("redis.database", 0));
			String password = config.getString("redis.password");
			if (StringUtils.isNotBlank(password)) {
				standaloneConfig.setPassword(RedisPassword.of(password));
			}
			factory = new JedisConnectionFactory(standaloneConfig);
		} else {
			// cluster mode
			RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
			for (String node : nodes) {
				String[] hostAndPort = StringUtils.split(node, ":");
				String host = StringUtils.trim(hostAndPort[0]);
				String port = StringUtils.trim(hostAndPort[1]);
				RedisClusterNode clusterNode = new RedisClusterNode(host, Integer.parseInt(port));
				clusterConfig.addClusterNode(clusterNode);
			}
			String password = config.getString("redis.password");
			if (StringUtils.isNotBlank(password)) {
				clusterConfig.setPassword(RedisPassword.of(password));
			}
			factory = new JedisConnectionFactory(clusterConfig, pool);
		}
		return factory;
	}

	@Bean(name = "redisTemplate")
	public RedisTemplate<String,Object> getRedisTemplate(@Qualifier("jedisConnectionFactory") JedisConnectionFactory connectionFactory){
		RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(Charset.forName("UTF-8")));
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		return redisTemplate;
	}
	
	@Bean(name = "stringRedisTemplate")
	public StringRedisTemplate getStringRedisTemplate(@Qualifier("jedisConnectionFactory") JedisConnectionFactory connectionFactory){
		return new StringRedisTemplate(connectionFactory);
	}
	
	@Bean(name = "cacheManager")
	public RedisCacheManager cacheManager(@Qualifier("jedisConnectionFactory") JedisConnectionFactory connectionFactory,
										  @Qualifier("redisPropertiesWrap") PropertiesWrap config) {
		
		RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration
				.defaultCacheConfig()
				.entryTtl(Duration.ofSeconds(config.getInteger("redis.expire", 60)))
				.prefixKeysWith(config.getString("redis.cacheManager.prefix", "jedis-redis-plugin:"))
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer(Charset.forName("UTF-8"))))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericFastJsonRedisSerializer()));
		return RedisCacheManager.builder(connectionFactory).cacheDefaults(redisCacheConfiguration).build();
	}
	
	@Bean(name = "redisPluginCacheService")
	public CacheService getCacheService() {
		return new RedisCacheService();
	}
}
