package io.github.pleuvoir.test.config;

import org.springframework.context.annotation.Configuration;

import io.github.pleuvoir.redis.autoconfigure.EnableRedisPlugin;
import io.github.pleuvoir.redis.autoconfigure.EnableRedisPlugin.Type;

@Configuration
@EnableRedisPlugin(type = Type.JEDIS)
public class AppConfiguration {

}
