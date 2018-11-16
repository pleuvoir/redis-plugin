package io.github.pleuvoir.redis.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EnableRedisPluginRegistrar.class)
public @interface EnableRedisPlugin {

	/**
	 * this is plugin name, it must be no-empty value.
	 */
	String name() default "redis-plugin";

	/**
	 * the location of resource file.
	 */
	String location() default "redis.properties";

}
