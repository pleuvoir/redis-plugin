package io.github.pleuvoir.redis.lock;

public interface Lock{

	/**
	 * 锁定，并发时第一个执行的锁会成功，其他的会失败
	 * 锁默认 5 秒后失效
	 * @return true 锁定成功，false 锁定失败
	 */
	boolean lock(String key);
	
	/**
	 * 锁定，并发时第一个执行的锁会成功，其他的会失败
	 * @param key	锁前缀
	 * @param timeout	超时时间，单位秒
	 * @return	true 锁定成功，false 锁定失败
	 */
	boolean lock(String key, String timeout);

	/**
	 * 解锁，该方法应该放置在<code>try...finally</code>块中
	 */
	void unlock(String key);

	/**
	 * 判断是否已被锁定
	 */
	boolean isLocked(String key);
}
