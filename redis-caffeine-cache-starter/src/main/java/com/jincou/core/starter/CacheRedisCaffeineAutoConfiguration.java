package com.jincou.core.starter;


import com.jincou.core.config.CacheRedisCaffeineProperties;
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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
@EnableConfigurationProperties(CacheRedisCaffeineProperties.class)
public class CacheRedisCaffeineAutoConfiguration {

	@Autowired
	private CacheRedisCaffeineProperties cacheRedisCaffeineProperties;

	@Bean
	@ConditionalOnBean(RedisTemplate.class)
	public RedisCaffeineCacheManager cacheManager(RedisTemplate<Object, Object> redisTemplate) {
		return new RedisCaffeineCacheManager(cacheRedisCaffeineProperties, redisTemplate);
	}

	@Bean
	@ConditionalOnMissingBean(name = "stringKeyRedisTemplate")
	public RedisTemplate<Object, Object> stringKeyRedisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
		RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
		template.setConnectionFactory(redisConnectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		return template;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<Object, Object> stringKeyRedisTemplate,
																	   RedisCaffeineCacheManager redisCaffeineCacheManager) {
		RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
		redisMessageListenerContainer.setConnectionFactory(stringKeyRedisTemplate.getConnectionFactory());
		CacheMessageListener cacheMessageListener = new CacheMessageListener(stringKeyRedisTemplate, redisCaffeineCacheManager);
		redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(cacheRedisCaffeineProperties.getRedis().getTopic()));
		return redisMessageListenerContainer;
	}
}
