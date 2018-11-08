package io.github.pleuvoir.test.limit;

import java.time.LocalDateTime;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.pleuvoir.redis.limit.RedisRateLimit;
import io.github.pleuvoir.test.config.AppConfiguration;

public class LuaLimitTest {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(AppConfiguration.class);
		RedisRateLimit limit = app.getBean(RedisRateLimit.class);

		
		String key;
		
		long start = System.currentTimeMillis();
		int i =0;
		while (limit.acquire(key = "ip:" + LocalDateTime.now().getSecond(), 10000)) {
			System.out.println("当前 key : " + key +" 第" + (++i) + "次访问中。。");
		}

		System.out.println("===========,cost:" + (System.currentTimeMillis()-start));

		app.close();
	}
}
