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
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;

import io.github.pleuvoir.base.kit.PropertiesWrap;
import io.github.pleuvoir.redis.cache.CacheService;
import io.github.pleuvoir.redis.cache.RedisCacheService;
import io.github.pleuvoir.redis.limit.LettuceRateLimit;
import io.github.pleuvoir.redis.lock.LettuceLock;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

@EnableCaching
@Import({ LettuceLock.class, LettuceRateLimit.class })
public class LettuceRedisConfiguration {

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
	
	@Bean(name = "clientResources", destroyMethod = "shutdown")
	public DefaultClientResources lettuceClientResources() {
		return DefaultClientResources.create();
	}
	
	@Bean(name = "lettuceConnectionFactory")
	public LettuceConnectionFactory getLettuceConnectionFactory(@Qualifier("redisPropertiesWrap") PropertiesWrap config,
																@Qualifier("clientResources") ClientResources clientResources) {
		
		String hostAndPortStr = config.getString("redis.hostAndPort");
		Assert.hasText(hostAndPortStr, "Redis configuration failed to get [hostAndPortStr]. Please check.");
		
		String[] nodes = StringUtils.split(hostAndPortStr, ",");
		
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().
															clientResources(clientResources)
															.build();
		LettuceConnectionFactory factory = null;
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
			factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
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
			clusterConfig.setMaxRedirects(nodes.length);
			factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
		}
		return factory;
	}
	
	@Bean(name = "redisTemplate")
	public RedisTemplate<String, Object> getRedisTemplate(@Qualifier("lettuceConnectionFactory") LettuceConnectionFactory connectionFactory) {
		RedisTemplate<String,Object> template = new RedisTemplate<>();
		template.setKeySerializer(new StringRedisSerializer(Charset.forName("UTF-8")));
		template.setValueSerializer(new GenericFastJsonRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer(Charset.forName("UTF-8")));
		template.setHashValueSerializer(new GenericFastJsonRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		return template;
	}

	@Bean(name = "stringRedisTemplate")
	public StringRedisTemplate getStringRedisTemplate(@Qualifier("lettuceConnectionFactory") LettuceConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}

	@Bean(name = "cacheManager")
	public RedisCacheManager cacheManager(@Qualifier("lettuceConnectionFactory") LettuceConnectionFactory connectionFactory,
										  @Qualifier("redisPropertiesWrap") PropertiesWrap config) {

		RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration
				.defaultCacheConfig()
				.entryTtl(Duration.ofSeconds(config.getInteger("redis.expire", 60)))
				.prefixKeysWith(config.getString("redis.cacheManager.prefix", "lettuce-redis-plugin:"))
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer(Charset.forName("UTF-8"))))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericFastJsonRedisSerializer()));
		return RedisCacheManager.builder(connectionFactory).cacheDefaults(redisCacheConfiguration).build();
	}
	
	@Bean(name = "redisPluginCacheService")
	public CacheService getCacheService() {
		return new RedisCacheService();
	}
}
