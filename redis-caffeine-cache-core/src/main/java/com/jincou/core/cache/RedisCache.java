package com.jincou.core.cache;

import com.alibaba.fastjson.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * redis工具类
 */
public class RedisCache {

    private RedisTemplate<Object, Object> redisTemplate;


    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }


    public void setRedisTemplate(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 向key值中设置value
     */
    public void set(String key,Object value,long timeout){
        redisTemplate.opsForValue().set(key, value, timeout,TimeUnit.MILLISECONDS);
    }
    /**
     * 向key值中设置value
     */
    public void set(String key,Object value){
        redisTemplate.opsForValue().set(key, value);
    }
    /**
     *
     * 获取value的值
     * @param key
     * @return Object
     */
    public Object get(String key){
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 向hash中添加map
     */
    public void hashAddAll(String key, Map<String, JSONObject> m){
        redisTemplate.opsForHash().putAll(key, m);
    }

    /**
     * 向hash中添加数据
     * @param key
     * @param filed
     * @param value
     */
    public void hashAdd(String key,String filed,Object value){
        redisTemplate.opsForHash().put(key, filed, value);
    }

    /***
     * 根据 key filed 查询一条数据
     * @param key
     * @param filed
     * @return Object
     */
    public Object hashSelectOne(String key,String filed){
        Object result =  redisTemplate.opsForHash().get(key,filed);
        return result;
    }

    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object,Object> hashSelectAll(String key){
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 根据key 删除对应的数据
     * @param key
     */
    public void delete(String key){
        redisTemplate.delete(key);
    }

    /**
     * 根据key filed 删除对应的hash中的数据
     * @param key
     * @param filed
     */
    public void deleteHashOne(String key,String filed){
        redisTemplate.opsForHash().delete(key, filed);
    }

    /**
     * 向set集合中添加数据
     * @param key
     * @param value
     * @return Long
     */
    public Long setAdd(String key,Object value){
        Long result = redisTemplate.opsForSet().add(key, value);
        return result;
    }

    /**
     * 根具key 获取set中的值
     * @param key
     * @return Set
     */
    public Set getSetData(String key){
        Set resultSet = redisTemplate.opsForSet().members(key);
        return resultSet;
    }

    /**
     * 判断 set 中是否存在某个值
     * @param key
     * @param value
     * @return boolean
     */
    public boolean isValiDateSet(String key,Object value){
        boolean resultBl = false;
        resultBl = redisTemplate.opsForSet().isMember(key, value);
        return resultBl;
    }

    /**
     * 获取 两个set的交集
     * @param key1
     * @param key2
     * @return Set
     */
    public Set intersect(String key1,String key2){
        Set resultSet = redisTemplate.opsForSet().intersect(key1, key2);
        return resultSet;
    }
    /**
     * 获取两个set的并集
     * @param key1
     * @param key2
     * @return
     */
    public Set unionSet(String key1,String key2){
        Set resultSet = redisTemplate.opsForSet().union(key1, key2);
        return resultSet;
    }
    /**
     * 向list中添加json字符串
     * @param key
     * @param value
     */
    public void listAdd(String key,String value){
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从list中取出json字符串
     * @param key
     * @return
     */
    public String listGet(String key){
        return (String) redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 向list中添加object
     * @param key
     * @param object
     */
    public void listAddObject(String key,Object object){
        redisTemplate.opsForList().leftPush(key, object);
    }

    /**
     * 获取list中的object
     * @param key
     * @return
     */
    public Object listGetObject(String key){
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 原子性增或减
     * @param key
     * @param value
     * @return
     */
    public long getSerialNumber(String key,long value){
        return redisTemplate.opsForValue().increment(key, value);
    }

    /**
     *
     * @param key
     * @param hashKey
     * @param value
     * @return
     */
    public long getSurplusNumber(String key,String hashKey,long value){
        return redisTemplate.opsForHash().increment(key, hashKey, value);
    }
    /**
     * 设置hashMap中可以的值
     * @param key
     * @param filed
     * @param value
     */
    public void hashPutSurplusNumber(String key,String filed,Long value){
        redisTemplate.opsForHash().put(key, filed, value.toString());
    }
    /**
     * 获取hash对应key中的值
     * @param key
     * @param filed
     * @return
     */
    public Object getSurplusNumber(String key,String filed){
        return  redisTemplate.opsForHash().get(key, filed);
    }

    /**
     * 自增
     * @param key
     * @param expire
     * @return
     */
    public long incr(String key,long expire){
        return redisTemplate.opsForValue().increment(key,expire);
    }

    /**
     * 返回符合给定模式的 key 列表
     * @param key
     * @return
     */
    public Set<String> keys(String key){
        Set keys = redisTemplate.keys(key + "*");
        return new HashSet<>(keys);
    }

    /**
     * 根据key列表批量获取value
     * @param keyList
     * @return
     */
    public List multiGet(List keyList){
        return redisTemplate.opsForValue().multiGet(keyList);
    }
}
