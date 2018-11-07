package io.github.pleuvoir.test.lock;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.pleuvoir.redis.lock.RedisLock;
import io.github.pleuvoir.test.config.AppConfiguration;

public class RedisLockTest {

	public static void main(String[] args) throws InterruptedException {

		AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(AppConfiguration.class);
		RedisLock lock = app.getBean(RedisLock.class);

		String key = "88250";
		boolean flag = lock.lock(key);
		System.out.println("加锁：" + flag);

		System.out.println("是否有锁：" + lock.isLocked(key));

		while (lock.isLocked(key)) {
			System.out.println("我被锁了。");
			TimeUnit.SECONDS.sleep(1);
		}

		System.out.println("========================");
		app.close();
	}
}
