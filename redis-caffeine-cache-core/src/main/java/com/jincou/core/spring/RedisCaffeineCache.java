package com.jincou.core.spring;

import com.github.benmanes.caffeine.cache.Cache;
import com.jincou.core.cache.RedisCache;
import com.jincou.core.config.L2CacheConfig;
import com.jincou.core.sync.CacheMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  TODO
 *
 * @author xub
 * @date 2022/3/16 下午3:12
 */
public class RedisCaffeineCache extends AbstractValueAdaptingCache {

	private final Logger logger = LoggerFactory.getLogger(RedisCaffeineCache.class);

	private String name;

	private RedisCache redisService;

	private Cache<Object, Object> caffeineCache;


	private long defaultExpiration = 0;

	private Map<String, Long> expires;

	private String topic = "cache:redis:caffeine:topic";

	private Map<String, ReentrantLock> keyLockMap = new ConcurrentHashMap<String, ReentrantLock>();

	protected RedisCaffeineCache(boolean allowNullValues) {
		super(allowNullValues);
	}

	public RedisCaffeineCache(String name, RedisCache redisService,
							  Cache<Object, Object> caffeineCache, L2CacheConfig l2CacheConfig) {
		super(l2CacheConfig.isAllowNullValues());
		this.name = name;
		this.redisService = redisService;
		this.caffeineCache = caffeineCache;
		this.defaultExpiration = l2CacheConfig.getRedis().getDefaultExpiration();
		this.expires = l2CacheConfig.getRedis().getExpires();
		this.topic = l2CacheConfig.getRedis().getTopic();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getNativeCache() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		Object value = lookup(key);
		if(value != null) {
			return (T) value;
		}

		ReentrantLock lock = keyLockMap.get(key.toString());
		if(lock == null) {
			logger.debug("create lock for key : {}", key);
			lock = new ReentrantLock();
			keyLockMap.putIfAbsent(key.toString(), lock);
		}
		try {
			lock.lock();
			value = lookup(key);
			if(value != null) {
				return (T) value;
			}
			value = valueLoader.call();
			Object storeValue = toStoreValue(value);
			put(key, storeValue);
			return (T) value;
		} catch (Exception e) {
			throw new ValueRetrievalException(key, valueLoader, e.getCause());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void put(Object key, Object value) {
		if (!super.isAllowNullValues() && value == null) {
			this.evict(key);
            return;
        }
		long expire = getExpire();
		if(expire > 0) {
			redisService.set(getKey(key),toStoreValue(value),expire);
		} else {
			redisService.set(getKey(key),toStoreValue(value));
		}

		push(new CacheMessage(this.name, key));

		caffeineCache.put(key, value);
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		String cacheKey = getKey(key);
		Object prevValue = null;
		// 考虑使用分布式锁，或者将redis的setIfAbsent改为原子性操作
		synchronized (key) {
			prevValue = redisService.get(cacheKey);
			if(prevValue == null) {
				long expire = getExpire();
				if(expire > 0) {
					redisService.set(getKey(key),toStoreValue(value),expire);
				} else {
					redisService.set(getKey(key),toStoreValue(value));
				}

				push(new CacheMessage(this.name, key));

				caffeineCache.put(key, toStoreValue(value));
			}
		}
		return toValueWrapper(prevValue);
	}

	@Override
	public void evict(Object key) {
		// 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
		redisService.delete(getKey(key));

		push(new CacheMessage(this.name, key));

		caffeineCache.invalidate(key);
	}

	@Override
	public void clear() {
		// 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
		Set<String> keys = redisService.keys(this.name.concat(":*"));
		for(String key : keys) {
			redisService.delete(key);
		}

		push(new CacheMessage(this.name, null));

		caffeineCache.invalidateAll();
	}

	@Override
	protected Object lookup(Object key) {
		String cacheKey = getKey(key);
		Object value = caffeineCache.getIfPresent(key);
		if(value != null) {
			logger.debug("get cache from caffeine, the key is : {}", cacheKey);
			return value;
		}

		value = redisService.get(cacheKey);

		if(value != null) {
			logger.debug("get cache from redis and put in caffeine, the key is : {}", cacheKey);
			caffeineCache.put(key, value);
		}
		return value;
	}

	private String getKey(Object key) {
		return this.name.concat(":").concat(":").concat(key.toString());
	}

	private long getExpire() {
		long expire = defaultExpiration;
		Long cacheNameExpire = expires.get(this.name);
		return cacheNameExpire == null ? expire : cacheNameExpire.longValue();
	}

	/**
	 * @description 缓存变更时通知其他节点清理本地缓存
	 * @author fuwei.deng
	 * @date 2018年1月31日 下午3:20:28
	 * @version 1.0.0
	 * @param message
	 */
	private void push(CacheMessage message) {
		redisService.getRedisTemplate().convertAndSend(topic, message);
	}

	/**
	 * @description 清理本地缓存
	 * @author fuwei.deng
	 * @date 2018年1月31日 下午3:15:39
	 * @version 1.0.0
	 * @param key
	 */
	public void clearLocal(Object key) {
		logger.debug("clear local cache, the key is : {}", key);
		if(key == null) {
			caffeineCache.invalidateAll();
		} else {
			caffeineCache.invalidate(key);
		}
	}
}
