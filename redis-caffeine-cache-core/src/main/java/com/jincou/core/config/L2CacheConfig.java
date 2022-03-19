package com.jincou.core.config;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.autoconfigure.cache.CacheType;
import java.util.*;

/**
 * @author chenck
 * @date 2020/6/30 17:19
 */
@Getter
@Setter
@Accessors(chain = true)
public class L2CacheConfig {


    /**
     * 是否存储空值，设置为true时，可防止缓存穿透
     */
    private boolean allowNullValues = true;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;


    private Set<String> cacheNames = new HashSet<>();


    private final Composite composite = new Composite();
    private final Caffeine caffeine = new Caffeine();
    private final Redis redis = new Redis();



    public interface Config {
    }

    /**
     * 组合缓存配置
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Composite implements Config {

        /**
         * 是否全部启用一级缓存，默认false
         */
        private boolean l1AllOpen = false;

        /**
         * 是否手动启用一级缓存，默认false
         */
        private boolean l1Manual = false;

        /**
         * 手动配置走一级缓存的缓存key集合，针对单个key维度
         */
        private Set<String> l1ManualKeySet = new HashSet<>();

        /**
         * 手动配置走一级缓存的缓存名字集合，针对cacheName维度
         */
        private Set<String> l1ManualCacheNameSet = new HashSet<>();
    }

    /**
     * Caffeine specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Caffeine implements Config {
        /**
         * 是否自动刷新过期缓存 true 表示是(默认)，false 表示否
         */
        private boolean autoRefreshExpireCache = false;

        /**
         * 缓存刷新调度线程池的大小
         * 默认为 CPU数 * 2
         */
        private Integer refreshPoolSize = Runtime.getRuntime().availableProcessors();

        /**
         * 缓存刷新的频率(秒)
         */
        private Long refreshPeriod = 30L;

        /**
         * 同一个key的发布消息频率(毫秒)
         */
        private Long publishMsgPeriodMilliSeconds = 500L;


        /** 访问后过期时间，单位秒*/
        private long expireAfterAccess;

        /** 写入后过期时间，单位秒*/
        private long expireAfterWrite;

        /** 写入后刷新时间，单位秒*/
        private long refreshAfterWrite;

        /** 初始化大小*/
        private int initialCapacity;

        /** 最大缓存对象个数，超过此数量时之前放入的缓存将失效*/
        private long maximumSize;
    }



    /**
     * Redis specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Redis implements Config {

        /** 全局过期时间，单位毫秒，默认不过期*/
        private long defaultExpiration = 0;

        /** 每个cacheName的过期时间，单位毫秒，优先级比defaultExpiration高*/
        private Map<String, Long> expires = new HashMap<>();

        /** 缓存更新时通知其他节点的topic名称*/
        private String topic = "cache:redis:caffeine:topic";

    }

}
