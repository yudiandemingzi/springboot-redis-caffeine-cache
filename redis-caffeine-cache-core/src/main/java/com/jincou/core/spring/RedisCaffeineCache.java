package com.jincou.core.spring;

import com.github.benmanes.caffeine.cache.Cache;
import com.jincou.core.cache.RedisCache;
import com.jincou.core.config.L2CacheConfig;
import com.jincou.core.sync.CacheMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.CollectionUtils;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO
 *
 * @author xub
 * @date 2022/3/16 下午3:12
 */
public class RedisCaffeineCache extends AbstractValueAdaptingCache {

	private final Logger logger = LoggerFactory.getLogger(RedisCaffeineCache.class);

	/**
	 * 缓存名称
	 */
	private String cacheName;

	/**
	 * 二级缓存实例
	 */
	private RedisCache redisCache;

	private Cache<Object, Object> caffeineCache;


	private long defaultExpiration = 0;

	private Map<String, Long> expires;

	private L2CacheConfig.Composite composite;

	/**
	 * 记录是否启用过一级缓存，只要启用过，则记录为true
	 * <p>
	 * 以下情况可能造成本地缓存与redis缓存不一致的情况 : 开启本地缓存，更新用户数据后，关闭本地缓存,更新用户信息到redis，开启本地缓存
	 * 解决方法：put、evict的情况下，判断配置中心一级缓存开关已关闭且本地一级缓存开关已开启的情况下，清除一级缓存
	 */
	private AtomicBoolean openedL1Cache = new AtomicBoolean();

	private String topic = "cache:redis:caffeine:topic";

	private Map<String, ReentrantLock> keyLockMap = new ConcurrentHashMap<String, ReentrantLock>();

	protected RedisCaffeineCache(boolean allowNullValues) {
		super(allowNullValues);
	}

	public RedisCaffeineCache(String cacheName, RedisCache redisCache,
							  Cache<Object, Object> caffeineCache, L2CacheConfig l2CacheConfig) {
		super(l2CacheConfig.isAllowNullValues());
		this.cacheName = cacheName;
		this.redisCache = redisCache;
		this.caffeineCache = caffeineCache;
		this.defaultExpiration = l2CacheConfig.getRedis().getDefaultExpiration();
		this.expires = l2CacheConfig.getRedis().getExpires();
		this.topic = l2CacheConfig.getRedis().getTopic();
		this.composite = l2CacheConfig.getComposite();

	}

	@Override
	public String getName() {
		return this.cacheName;
	}

	@Override
	public Object getNativeCache() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		Object value = lookup(key);
		if (value != null) {
			return (T) value;
		}

		ReentrantLock lock = keyLockMap.get(key.toString());
		if (lock == null) {
			logger.debug("create lock for key : {}", key);
			lock = new ReentrantLock();
			keyLockMap.putIfAbsent(key.toString(), lock);
		}
		try {
			lock.lock();
			value = lookup(key);
			if (value != null) {
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
		if (expire > 0) {
			redisCache.set(getKey(key), toStoreValue(value), expire);
		} else {
			redisCache.set(getKey(key), toStoreValue(value));
		}

		push(new CacheMessage(this.cacheName, key));

		caffeineCache.put(key, value);
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		String cacheKey = getKey(key);
		Object prevValue = null;
		// 考虑使用分布式锁，或者将redis的setIfAbsent改为原子性操作
		synchronized (key) {
			prevValue = redisCache.get(cacheKey);
			if (prevValue == null) {
				long expire = getExpire();
				if (expire > 0) {
					redisCache.set(getKey(key), toStoreValue(value), expire);
				} else {
					redisCache.set(getKey(key), toStoreValue(value));
				}

				push(new CacheMessage(this.cacheName, key));

				caffeineCache.put(key, toStoreValue(value));
			}
		}
		return toValueWrapper(prevValue);
	}

	@Override
	public void evict(Object key) {
		// 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
		redisCache.delete(getKey(key));

		push(new CacheMessage(this.cacheName, key));

		caffeineCache.invalidate(key);
	}

	@Override
	public void clear() {
		// 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
		Set<String> keys = redisCache.keys(this.cacheName.concat(":*"));
		for (String key : keys) {
			redisCache.delete(key);
		}

		push(new CacheMessage(this.cacheName, null));

		caffeineCache.invalidateAll();
	}

	@Override
	protected Object lookup(Object key) {
		String cacheKey = getKey(key);
		Object value = caffeineCache.getIfPresent(key);
		if (value != null) {
			logger.debug("get cache from caffeine, the key is : {}", cacheKey);
			return value;
		}

		value = redisCache.get(cacheKey);

		if (value != null) {
			logger.debug("get cache from redis and put in caffeine, the key is : {}", cacheKey);
			caffeineCache.put(key, value);
		}
		return value;
	}

	private String getKey(Object key) {
		return this.cacheName.concat(":").concat(key.toString());
	}

	private long getExpire() {
		long expire = defaultExpiration;
		Long cacheNameExpire = expires.get(this.cacheName);
		return cacheNameExpire == null ? expire : cacheNameExpire.longValue();
	}

	/**
	 * @param message
	 * @description 缓存变更时通知其他节点清理本地缓存
	 * @author fuwei.deng
	 * @date 2018年1月31日 下午3:20:28
	 * @version 1.0.0
	 */
	private void push(CacheMessage message) {
		redisCache.getRedisTemplate().convertAndSend(topic, message);
	}

	/**
	 * @param key
	 * @description 清理本地缓存
	 * @author fuwei.deng
	 * @date 2018年1月31日 下午3:15:39
	 * @version 1.0.0
	 */
	public void clearLocal(Object key) {
		logger.debug("clear local cache, the key is : {}", key);
		if (key == null) {
			caffeineCache.invalidateAll();
		} else {
			caffeineCache.invalidate(key);
		}
	}

	/**
	 * 查询是否开启一级缓存
	 *
	 * @param key 缓存key
	 * @return
	 */
	private boolean ifL1Open(Object key) {
		// 检测开关与缓存名称
		if (ifL1Open()) {
			return true;
		}

		// 检测key
		return ifL1OpenByKey(key);
	}

	/**
	 * 本地缓存检测，检测开关与缓存名称
	 *
	 * @return
	 */
	private boolean ifL1Open() {
		// 判断是否开启过本地缓存
		if (composite.isL1AllOpen() || composite.isL1Manual()) {
			openedL1Cache.compareAndSet(false, true);
		}
		// 是否启用一级缓存
		if (composite.isL1AllOpen()) {
			return true;
		}
		// 是否使用手动匹配开关
		if (composite.isL1Manual()) {
			// 手动匹配缓存名字集合，针对cacheName维度
			Set<String> l1ManualCacheNameSet = composite.getL1ManualCacheNameSet();
			if (!CollectionUtils.isEmpty(l1ManualCacheNameSet) && composite.getL1ManualCacheNameSet().contains(this.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 本地缓存检测，检测key
	 *
	 * @param key
	 * @return
	 */
	private boolean ifL1OpenByKey(Object key) {
		// 是否使用手动匹配开关
		if (composite.isL1Manual()) {
			// 手动匹配缓存key集合，针对单个key维度
			Set<String> l1ManualKeySet = composite.getL1ManualKeySet();
			if (!CollectionUtils.isEmpty(l1ManualKeySet) && l1ManualKeySet.contains(getKey(key))) {
				return true;
			}
		}

		return false;
	}
}
