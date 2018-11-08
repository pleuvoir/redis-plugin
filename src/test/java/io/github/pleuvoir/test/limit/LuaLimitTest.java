package io.github.pleuvoir.test.limit;

import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.pleuvoir.redis.limit.LettuceRedisRateLimit;
import io.github.pleuvoir.test.config.AppConfiguration;

public class LuaLimitTest {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(AppConfiguration.class);
		LettuceRedisRateLimit limitExecutor = app.getBean(LettuceRedisRateLimit.class);
		
	       Timer timer1 = new Timer();
	        timer1.scheduleAtFixedRate(new TimerTask() {
	            public void run() {
	                for (int i = 0; i < 3; i++) {
	                    new Thread(new Runnable() {
	                        @Override
	                        public void run() {
	                            try {
	                                System.out.println("Limit=" +  limitExecutor.tryAccess("limit", "X-Y", 10, 5));
	                            } catch (Exception e) {
	                                e.printStackTrace();
	                            }
	                        }
	                    }).start();
	                }
	            }
	        }, 0L, 1000L);
	        
//		for (int j = 0; j < 10; j++) {
//			boolean tryAccess = limit.tryAccess("fw", "fw-key", 1, 50);
//			if(!tryAccess) {
//				System.out.println("被拒绝了");
//			}else {
//				System.out.println("通过了");
//			}
//		}

	//	app.close();
	}
}
