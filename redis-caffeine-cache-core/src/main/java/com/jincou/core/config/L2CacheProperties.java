package com.jincou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;



/**
 *  一二级缓存属性配置
 *
 * @author xub
 * @date 2022/3/17 上午9:34
 */
@ConfigurationProperties(prefix = "l2cache")
public class L2CacheProperties {
    /**
     * 缓存配置
     */
    private L2CacheConfig config;

    public L2CacheConfig getConfig() {
        return config;
    }

    public void setConfig(L2CacheConfig config) {
        this.config = config;
    }
}
