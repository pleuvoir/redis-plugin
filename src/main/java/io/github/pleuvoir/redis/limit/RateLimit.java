package io.github.pleuvoir.redis.limit;

public interface RateLimit {

	/**
	 * 是否成功，如果失败则代表当前热点资源已到达设置的最大访问次数
	 * @param key	热点资源 key
	 * @param maxTimes	最大访问次数
	 * @return	true 代表可以继续访问， false 代表已经到达最大访问次数
	 */
	boolean acquire(String key, int maxTimes);
}
