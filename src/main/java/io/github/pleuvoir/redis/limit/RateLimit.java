package io.github.pleuvoir.redis.limit;

public interface RateLimit {

	/**
	* 在给定的时间段里最多的访问限制次数(超出次数返回false)；等下个时间段开始，才允许再次被访问(返回true)，周而复始
	* @param name 资源名字
	* @param key 资源Key
	* @param limitPeriod 给定的时间段(单位秒)
	* @param limitCount 最多的访问限制次数
	* @return boolean 超出次数返回false
	*/
	boolean tryAccess(String name, String key, int limitPeriod, int limitCount);
}
