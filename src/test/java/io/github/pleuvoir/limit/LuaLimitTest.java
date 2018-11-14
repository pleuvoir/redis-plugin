package io.github.pleuvoir.limit;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.pleuvoir.config.AppConfiguration;
import io.github.pleuvoir.redis.limit.RateLimit;

public class LuaLimitTest {

	public static void main(String[] args) throws InterruptedException {

		AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(AppConfiguration.class);
		RateLimit limitExecutor = app.getBean(RateLimit.class);
		
	       Timer timer = new Timer();
	        timer.scheduleAtFixedRate(new TimerTask() {
	            public void run() {
	                for (int i = 0; i < 1; i++) {
	                    new Thread(new Runnable() {
	                        @Override
	                        public void run() {
							if (limitExecutor.tryAccess("limit", "X-Y", 10, 3)) {
								System.out.println("I get it ! " + LocalDateTime.now().getSecond());
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
}
