package io.github.pleuvoir.redis.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;

import io.github.pleuvoir.redis.config.LettuceRedisConfiguration;

public class EnableRedisPluginRegistrar extends AbstractPluginRegistrar {

	@Override
	protected Class<? extends Annotation> getEnableAnnotationClass() {
		return EnableRedisPlugin.class;
	}

	@Override
	protected Class<?> defaultConfigurationClass() {
		return LettuceRedisConfiguration.class;
	}

	@Override
	protected void customize(BeanDefinitionRegistry registry, AnnotationAttributes attributes,
			BeanDefinitionBuilder definition, BeanFactory beanFactory) {
		definition.addPropertyValue("location", getLocationWithProfileIfNecessary(attributes.getString("location")));
	}

}
