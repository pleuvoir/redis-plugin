package io.github.pleuvoir.lock;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.pleuvoir.config.AppConfiguration;
import io.github.pleuvoir.redis.lock.Lock;

public class RedisLockTest {

	static int taskCount = 2;

	static CyclicBarrier cyclicBarrier = new CyclicBarrier(taskCount);
	
	static Lock lock;

	public static void main(String[] args) throws InterruptedException {

		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(AppConfiguration.class);
		lock = app.getBean(Lock.class);


		for (int i = 0; i < taskCount; i++) {
			new ExcuteThread().start();
		}

	}

	static class ExcuteThread extends Thread {

		@Override
		public void run() {
			try {
				if (new Random().nextBoolean()) {
					try {
						TimeUnit.SECONDS.sleep(2);
					} catch (InterruptedException e) {
					}
					System.out.println(this.getName() + " 运气不好，休息 2 秒");
				}

				System.out.println(this.getName() + " 到达屏障前");
				cyclicBarrier.await();

				System.out.println(this.getName() + "准备休息 2 秒，然后一起出发");
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
				}
				System.out.println(this.getName() + " 到达位置");
				bussiness();
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
	}

	private static void bussiness() {
		String name = Thread.currentThread().getName();
		String key = "88250";

		if (lock.isLocked(key)) {
			System.out.println(name + "	😭  this resource is locked .. ");
			return;
		}
		if (!lock.lock(key)) {
			System.out.println(name + "	I got a lock fail ... " );
			return;
		}
		try {
			
			// do your bussiness
			System.out.println(Thread.currentThread().getName() + " ============ unpark happily ... ============");
		} finally {
			System.out.println(name + "	unlock " );
			lock.unlock(key);
		}
	}

}
