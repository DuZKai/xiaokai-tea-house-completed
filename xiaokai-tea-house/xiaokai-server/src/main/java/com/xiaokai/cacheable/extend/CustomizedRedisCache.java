package com.xiaokai.cacheable.extend;

import cn.hutool.core.util.ObjectUtil;
import com.xiaokai.cacheable.extend.model.CachedInvocation;
import com.xiaokai.cacheable.extend.utils.CacheHelper;
import com.xiaokai.cacheable.extend.utils.ThreadPoolUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @description: 自定义redis缓存，重写get方法
 **/
@Slf4j
public class CustomizedRedisCache extends RedisCache {
    // 声明一个可重入锁 lock，用于确保在执行缓存刷新时，不会出现多线程竞争的情况，从而保证线程安全
    private ReentrantLock lock = new ReentrantLock();

    // 调用了 RedisCache 的构造函数，接收缓存名称 name，RedisCacheWriter，以及 RedisCacheConfiguration 作为参数
    // 保留了原生 RedisCache 的基本功能，并可以在此基础上进行扩展
    public CustomizedRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
        super(name, cacheWriter, cacheConfig);
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        // 调用父类 RedisCache 的 get 方法，获取缓存中与传入 key 对应的值，并将其存储在 valueWrapper 中
        ValueWrapper valueWrapper = super.get(key);
        // 获取当前缓存调用的元数据信息，通常包含缓存名称、缓存键、过期时间等信息
        CachedInvocation cachedInvocation = CacheHelper.getCacheManager().getCachedInvocation();
        // 获取预加载时间 preLoadTimeSecond，表示缓存到期之前多少秒需要进行缓存刷新
        long preLoadTimeSecond = 0;
        if(ObjectUtil.isNotEmpty(cachedInvocation))
            preLoadTimeSecond = cachedInvocation.getMetaData().getPreLoadTimeSecond();
        if (ObjectUtil.isNotEmpty(valueWrapper) && preLoadTimeSecond > 0) {
            // 生成 Redis 中实际使用的缓存键，通常由缓存名称和 key 组合生成
            String cacheKey = createCacheKey(key);
            // 从缓存管理器中获取 RedisTemplate，用于与 Redis 进行交互
            RedisTemplate cacheRedisTemplate = CacheHelper.getCacheManager().getCacheRedisTemplate();
            // 获取缓存项的剩余存活时间（TTL，Time-To-Live），单位为秒
            Long ttl = cacheRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (ObjectUtil.isNotEmpty(ttl) && ttl <= preLoadTimeSecond) {
                log.info("缓存刷新中...");
                log.info("cacheKey：{}, ttl: {}, preLoadTimeSecond: {}", cacheKey, ttl, preLoadTimeSecond);
                // 将缓存刷新任务提交到线程池中执行，保证不会阻塞当前主线程
                ThreadPoolUtils.execute(() -> {
                    // 在刷新缓存时使用重入锁，确保同一缓存不会被多个线程同时刷新，避免并发问题
                    lock.lock();
                    try {
                        // 调用 CacheHelper 中的 refreshCache 方法，执行实际的缓存刷新操作
                        CacheHelper.refreshCache(super.getName());
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    } finally {
                        lock.unlock();
                    }
                });
            }

        }
        // 无论是否触发了缓存刷新操作，都将缓存中获取的值（valueWrapper）返回给调用方
        return valueWrapper;
    }

}
