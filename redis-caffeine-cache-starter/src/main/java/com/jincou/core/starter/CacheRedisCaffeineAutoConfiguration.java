package com.jincou.core.starter;



import com.jincou.core.cache.RedisCache;
import com.jincou.core.config.L2CacheProperties;
import com.jincou.core.spring.RedisCaffeineCacheManager;
import com.jincou.core.sync.CacheMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.net.UnknownHostException;


/**
 *  TODO
 *
 * @author xub
 * @date 2022/3/16 下午3:13
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(L2CacheProperties.class)
public class CacheRedisCaffeineAutoConfiguration {

	@Autowired
	private L2CacheProperties l2CacheProperties;

	@Bean
	@ConditionalOnClass(RedisCache.class)
	@Order(2)
	public RedisCaffeineCacheManager cacheManager(RedisCache redisCache) {
		return new RedisCaffeineCacheManager(l2CacheProperties.getConfig(),redisCache);
	}

	@Bean
	@ConditionalOnMissingBean(name = "stringKeyRedisTemplate")
	public RedisTemplate<Object, Object> stringKeyRedisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		RedisSerializer stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);
		return template;
	}

	@Bean
	@ConditionalOnClass(RedisCache.class)
	@Order(3)
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisCache redisCache,
																	   RedisCaffeineCacheManager cacheManager) {
		RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
		redisMessageListenerContainer.setConnectionFactory(redisCache.getRedisTemplate().getConnectionFactory());
		CacheMessageListener cacheMessageListener = new CacheMessageListener(redisCache, cacheManager);
		redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(l2CacheProperties.getConfig().getRedis().getTopic()));
		return redisMessageListenerContainer;
	}

	@Bean
	@ConditionalOnBean(RedisTemplate.class)
	@Order(1)
	public RedisCache redisCache(RedisTemplate<Object, Object> stringKeyRedisTemplate) {
		RedisCache redisCache = new RedisCache();
		redisCache.setRedisTemplate(stringKeyRedisTemplate);
		return redisCache;
	}
}
