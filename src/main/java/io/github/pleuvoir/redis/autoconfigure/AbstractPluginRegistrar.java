package io.github.pleuvoir.redis.autoconfigure;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

public abstract class AbstractPluginRegistrar
		implements BeanFactoryAware, ImportBeanDefinitionRegistrar, EnvironmentAware {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected static final String PROPERTIES_FILE_EXTENSION = ".properties";

	private BeanFactory beanFactory;

	private Environment environment;
	
	private Class<?> configurationClass;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(getEnableAnnotationClassName(), false));

		Assert.notNull(attributes, "No " + getEnableAnnotationClassName() + " attributes found. Is "
				+ metadata.getClassName() + " annotated with @" + getEnableAnnotationClassName() + "?");

		this.configurationClass = defaultConfigurationClass();
		
		// give sub-class a change to correct main configuration and others...
		if (!correct(attributes, this.beanFactory)) {
			return;
		}
		
		Assert.notNull(this.configurationClass, "Plugin main Configuration Class must be null!");
		
		String pluginName = attributes.getString("name");

		if (registry.containsBeanDefinition(StringUtils.uncapitalize(configurationClass.getSimpleName()))) {
			logger.warn("[" + pluginName + "] mainConfigurationClass " + configurationClass.getSimpleName()
					+ " has been initialized, it is not recommended to use @" + getEnableAnnotationClassName() + ".");
		}

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(configurationClass);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

		customize(registry, attributes, definition, this.beanFactory);

		registry.registerBeanDefinition(pluginName, definition.getBeanDefinition());
	}

	private String getEnableAnnotationClassName() {
		return getEnableAnnotationClass().getCanonicalName();
	}

	protected String getLocationWithProfileIfNecessary(String location) {
		String[] activeProfiles = environment.getActiveProfiles();
		if (ArrayUtils.isEmpty(activeProfiles)) {
			return location;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(StringUtils.substringBefore(location, PROPERTIES_FILE_EXTENSION)).append("-")
				.append(activeProfiles[0]).append(PROPERTIES_FILE_EXTENSION);

		if (logger.isInfoEnabled()) {
			logger.info("activeProfiles： {}, location：{}", Arrays.asList(activeProfiles), builder.toString());
		}
		return builder.toString();
	}

	protected void customize(Map<String, Object> attributes, BeanDefinitionBuilder definition) {
		for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
			definition.addPropertyValue(attribute.getKey(), attribute.getValue());
		}
	}
	
	/**
	 * give sub-class a change to correct main configuration and others...
	 * @param attributes	annotation attributes
	 * @param beanFactory	the root interface for accessing a Spring bean container.
	 * @return	if false, plugin register will break.
	 */
	protected abstract boolean correct(AnnotationAttributes attributes, BeanFactory beanFactory);

	protected abstract void customize(BeanDefinitionRegistry registry, AnnotationAttributes attributes,
			BeanDefinitionBuilder definition, BeanFactory beanFactory);

	/**
	 * The annotation used to enable the particular plugin support.
	 * @return the annotation class
	 */
	protected abstract Class<? extends Annotation> getEnableAnnotationClass();

	/**
	 * The default configuration class that will be used by plugins-support as booting.
	 * @return the default configuration class
	 */
	protected abstract Class<?> defaultConfigurationClass();
	
	public Class<?> getConfigurationClass() {
		return configurationClass;
	}

	public void setConfigurationClass(Class<?> configurationClass) {
		this.configurationClass = configurationClass;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}
}
