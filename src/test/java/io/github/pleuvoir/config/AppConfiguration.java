package io.github.pleuvoir.config;

import org.springframework.context.annotation.Configuration;

import io.github.pleuvoir.redis.autoconfigure.EnableRedisPlugin;
import io.github.pleuvoir.redis.autoconfigure.EnableRedisPlugin.Type;

@Configuration
@EnableRedisPlugin(type = Type.LETTUCE)
public class AppConfiguration {

}
