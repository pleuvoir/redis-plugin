package io.github.pleuvoir.lock;

import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.pleuvoir.config.AppConfiguration;
import io.github.pleuvoir.redis.lock.Lock;

public class RedisLockTest {

	public static void main(String[] args) throws InterruptedException {

		AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(AppConfiguration.class);
		Lock lock = app.getBean(Lock.class);

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				for (int i = 0; i < 15; i++) {
					new Thread(new Runnable() {
						@Override
						public void run() {
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
						}
					}).start();
				}
			}
		}, 0L, 1000);

		synchronized (timer) {
			timer.wait();
		}
		app.close();

	}


	private static void unpark() {
		System.out.println("============ unpark happily ... ============");
	}
}
