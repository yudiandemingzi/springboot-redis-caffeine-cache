package com.jincou.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  配置信息
 *
 * @author xub
 * @date 2022/3/16 上午10:30
 */
//@ConfigurationProperties(prefix = "spring.cache.multi")
public class CacheRedisCaffeineProperties {

	private Set<String> cacheNames = new HashSet<>();

	/** 是否存储空值，默认true，防止缓存穿透*/
	private boolean cacheNullValues = true;

	/** 是否动态根据cacheName创建Cache的实现，默认true*/
	private boolean dynamic = true;

	/** 缓存key的前缀*/
	private String cachePrefix;

	private Redis redis = new Redis();

	private Caffeine caffeine = new Caffeine();

	@Data
	public class Redis {

		/** 全局过期时间，单位毫秒，默认不过期*/
		private long defaultExpiration = 0;

		/** 每个cacheName的过期时间，单位毫秒，优先级比defaultExpiration高*/
		private Map<String, Long> expires = new HashMap<>();

		/** 缓存更新时通知其他节点的topic名称*/
		private String topic = "cache:redis:caffeine:topic";
	}

	@Data
	public class Caffeine {

		/** 访问后过期时间，单位毫秒*/
		private long expireAfterAccess;

		/** 写入后过期时间，单位毫秒*/
		private long expireAfterWrite;

		/** 写入后刷新时间，单位毫秒*/
		private long refreshAfterWrite;

		/** 初始化大小*/
		private int initialCapacity;

		/** 最大缓存对象个数，超过此数量时之前放入的缓存将失效*/
		private long maximumSize;

		/** 由于权重需要缓存对象来提供，对于使用spring cache这种场景不是很适合，所以暂不支持配置*/
//		private long maximumWeight;

	}

	public Set<String> getCacheNames() {
		return cacheNames;
	}

	public void setCacheNames(Set<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	public boolean isCacheNullValues() {
		return cacheNullValues;
	}

	public void setCacheNullValues(boolean cacheNullValues) {
		this.cacheNullValues = cacheNullValues;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public String getCachePrefix() {
		return cachePrefix;
	}

	public void setCachePrefix(String cachePrefix) {
		this.cachePrefix = cachePrefix;
	}

	public Redis getRedis() {
		return redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

	public Caffeine getCaffeine() {
		return caffeine;
	}

	public void setCaffeine(Caffeine caffeine) {
		this.caffeine = caffeine;
	}
}
