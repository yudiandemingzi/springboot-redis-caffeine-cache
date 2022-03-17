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
     * NullValue的过期时间，单位秒，默认30秒
     * 用于淘汰NullValue的值
     * 注：当缓存项的过期时间小于该值时，则NullValue不会淘汰
     */
    private long nullValueExpireTimeSeconds = 60;

    /**
     * NullValue 的最大数量，防止出现内存溢出
     * 注：当超出该值时，会在下一次刷新缓存时，淘汰掉NullValue的元素
     */
    private long nullValueMaxSize = 3000;

    /**
     * NullValue 的清理频率(秒)
     */
    private long nullValueClearPeriodSeconds = 10L;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;

    /**
     * 缓存类型，默认 COMPOSITE 组合缓存
     *
     * @see CacheType
     */
    //private String cacheType = CacheType.COMPOSITE.name();
    private Set<String> cacheNames = new HashSet<>();


    private final Composite composite = new Composite();
    private final Caffeine caffeine = new Caffeine();
    private final Redis redis = new Redis();
    private final CacheSyncPolicy cacheSyncPolicy = new CacheSyncPolicy();


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
         * 一级缓存类型
         */
        private String l1CacheType = CacheType.CAFFEINE.name();
        /**
         * 二级缓存类型
         */
        private String l2CacheType = CacheType.REDIS.name();
        /**
         * CompositeCache.batchPut()中往l2中是通过batchPut()还是循环put()来缓存数据，默认false
         * true 表示调用l2的batchPut()来缓存数据
         * false 表示循环调用l2的put()来缓存数据
         */
        private boolean l2BatchPut = false;
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



        /**
         * The spec to use to create caches. See CaffeineSpec for more details on the spec format.
         */
        private String defaultSpec;

        /**
         * The spec to use to create caches. See CaffeineSpec for more details on the spec format.
         * <key,value>=<cacheName, spec>
         */
        private Map<String, String> specs = new HashMap<>();

        /**
         * 是否启用自定义 MdcForkJoinPool，用于链路追踪
         */
        private boolean enableMdcForkJoinPool = true;


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

        /**
         * 加载数据时，是否加锁
         */
        private boolean lock = false;

        /**
         * 加载数据时是调用tryLock()，还是lock()
         * 注：
         * tryLock() 只有一个请求执行加载动作，其他并发请求，直接返回失败
         * lock() 只有一个请求执行加载动作，其他并发请求，会阻塞直到获得锁
         */
        private boolean tryLock = true;

        /**
         * 缓存过期时间(ms)
         * 注：作为默认的缓存过期时间，如果一级缓存设置了过期时间，则以一级缓存的过期时间为准。
         * 目的是为了支持cacheName维度的缓存过期时间设置
         */
        private long expireTime;

        /**
         * 针对cacheName维度过期时间集合
         * <cacheName,过期时间(ms)>
         */
        private Map<String, Long> expireTimeCacheNameMap = new HashMap<>();

        /**
         * 批量操作的大小，可以理解为是分页
         */
        private int batchPageSize = 50;



        /**
         * 是否启用副本，默认false
         * 主要解决单个redis分片上热点key的问题，相当于原来存一份数据，现在存多份相同的数据，将热key的压力分散到多个分片。
         * 以redis内存空间来降低单分片压力。
         */
        private boolean duplicate = false;

        /**
         * 针对所有key启用副本
         */
        private boolean duplicateALlKey = false;

        /**
         * 默认副本数量
         */
        private int defaultDuplicateSize = 10;

        /**
         * 副本缓存key集合，针对单个key维度
         * <key,副本数量>
         */
        private Map<String, Integer> duplicateKeyMap = new HashMap<>();

        /**
         * 副本缓存名字集合，针对cacheName维度
         * <cacheName,副本数量>
         */
        private Map<String, Integer> duplicateCacheNameMap = new HashMap<>();


    }

    /**
     * 缓存同步策略配置
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class CacheSyncPolicy implements Config {

        /**
         * 策略类型
         *
         * @see CacheSyncPolicyType
         */
        private String type;

        /**
         * 缓存更新时通知其他节点的topic名称
         */
        private String topic = "l2cache";

        /**
         * 是否支持异步发送消息
         */
        private boolean isAsync;

        /**
         * 具体的属性配置
         * 定义一个通用的属性字段，不同的MQ可配置各自的属性即可。
         * 如:kafka 的属性配置则完全与原生的配置保持一致
         */
        private Properties props = new Properties();
    }

}
