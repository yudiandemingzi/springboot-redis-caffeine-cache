package com.jincou.core.starter;



import com.jincou.core.cache.RedisCache;
import com.jincou.core.config.L2CacheProperties;
import com.jincou.core.spring.RedisCaffeineCacheManager;
import com.jincou.core.sync.CacheMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
	@ConditionalOnBean(RedisCache.class)
	@Order(2)
	public RedisCaffeineCacheManager cacheManager(RedisCache redisService) {
		return new RedisCaffeineCacheManager(l2CacheProperties.getConfig(),redisService);
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
	@ConditionalOnBean(RedisCache.class)
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisCache redisService,
																	   RedisCaffeineCacheManager redisCaffeineCacheManager) {
		RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
		redisMessageListenerContainer.setConnectionFactory(redisService.getRedisTemplate().getConnectionFactory());
		CacheMessageListener cacheMessageListener = new CacheMessageListener(redisService, redisCaffeineCacheManager);
		redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(l2CacheProperties.getConfig().getCacheSyncPolicy().getTopic()));
		return redisMessageListenerContainer;
	}

	@Bean
	@ConditionalOnBean(RedisTemplate.class)
	@Order(1)
	public RedisCache redisService(RedisTemplate<Object, Object> stringKeyRedisTemplate) {
		RedisCache redisService = new RedisCache();
		redisService.setRedisTemplate(stringKeyRedisTemplate);
		return redisService;
	}
}
