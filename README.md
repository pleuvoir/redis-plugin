
## :rocket: 方便的使用 redis

[![HitCount](http://hits.dwyl.io/pleuvoir/redis-plugin.svg)](http://hits.dwyl.io/pleuvoir/redis-plugin) 
[![GitHub issues](https://img.shields.io/github/issues/pleuvoir/redis-plugin.svg)](https://github.com/pleuvoir/redis-plugin/issues)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?label=license)](https://github.com/pleuvoir/redis-plugin/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.pleuvoir/redis-plugin.svg?label=maven%20central)](https://oss.sonatype.org/#nexus-search;quick~redis-plugin)
[![Download](https://img.shields.io/badge/downloads-master-green.svg)](https://codeload.github.com/pleuvoir/redis-plugin/zip/master)

### 特性

- 简单易用
- 支持集群
- 自动配置
- 多种实现可自由切换
- 方便的 API
- 分布式锁
- 分布式限流

### 快速开始

#### 1.引入依赖

```xml
<dependency>
	<groupId>io.github.pleuvoir</groupId>
	<artifactId>redis-plugin</artifactId>
	<version>${latest.version}</version>
</dependency>
```

#### 2. 配置文件

接着我们需要准备一份配置文件，它看起来是这样的，文件的名称我们先假定为 `redis.properties`

此处 `redis.hostAndPort=127.0.0.1:6379` 代表单机，如果是集群可以是 `127.0.0.1:6379,127.0.0.1:6379,127.0.0.1:6379` 这样的格式。

```xml
redis.hostAndPort=127.0.0.1:6379
redis.database=1
redis.password=
redis.pool.maxIdle=4
redis.pool.maxTotal=6
redis.pool.maxWait=5000
redis.cacheManager.prefix=redis-plugin:
```

#### 3. 使用 spring 进行管理

对于使用 `xml` 进行配置的项目，只需要如下声明，即可获得缓存能力。

```xml
<bean class="io.github.pleuvoir.JedisRedisConfiguration">
    <property name="location" value="redis.properties"/>
</bean>
```

显然，这种实现是基于 `Jedis` 的，同时我们也支持 `Lettuce`，就像这样：

```xml
<bean class="io.github.pleuvoir.LettuceRedisConfiguration">
    <property name="location" value="redis.properties"/>
</bean>
```

提示：使用 xml 注册的方式，可以不指定扫描包。

如果是使用注解的项目，建议使用自动配置。

只需在配置类中声明 `@EnableRedisPlugin` 即可，当然这是使用默认的配置。 `EnableRedisPlugin` 注解有几个重要的属性，分别是 `location` 以及 `Type`，其中 `location` 表示需要加载的配置文件位置，`location` 可以不声明，默认为 classpath 下的 `redis.properties` 文件。 `Type` 则表示可以选择内部的第三方  `redis` 实现，默认是 `Lettuce` ，目前支持 `Jedis` 和  `Lettuce`。

#### 4. API

配置完成后，缓存服务 `CacheService` 提供了一些操作数据的方法，详情请查看具体 API：

```java
/**
 * 添加缓存，使用默认失效时间
 * @param key
 * @param value
 */
public void set(String key, Object value);

/**
 * 取缓存
 * @param key
 * @return Object
 */
public Object get(String key);

/**
 * 当缓存中没有时存入，缓存中存在时不存入
 * @param key
 * @param value
 * @return 缓存中没有时返回true，缓存中有时返回false
 */
boolean putIfExist(String key, Object value);

...
```

#### 5. 分布式锁

锁的使用方法如下所指：

```java
String key = "88250";

if (lock.isLocked(key)) {
	System.out.println("😭  this resource is locked .. ");
	return;
}

try {
	if (!lock.lock(key)) {
		System.out.println("I got a lock fail ...");
		return;
	}
	// do your bussiness
	unpark();
} finally {
	lock.unlock(key);
}
```

#### 6. 限流

```java
limitExecutor.tryAccess("limit", "X-Y", 10, 3);
```

流控正常时返回  `true`，被限流时返回 `false`，其中 `limit` 为资源的名称， `X-Y` 为限流 key ， 10 和  3 代表 <b> 该资源 10 秒内可以访问 3 次</b>。

### 特别说明

如果项目使用  `Profiles` 来管理 spring 的环境，例如  `Environment().setActiveProfiles("dev")` ，自动配置会尝试将当前环境修饰符追加到文件名称后，即如果您使用了 `@EnableRedisPlugin(location = "redis.properties")` 进行自动配置，插件会去寻找名为   `redis-dev.properties` 的配置文件，确保文件存在即可。

使用 xml 注册的方式，不受此特性的影响。

### TODO LIST

- [ ] 消息中间件
- [ ] More API

### 开源协议
[Apache License](LICENSE)


