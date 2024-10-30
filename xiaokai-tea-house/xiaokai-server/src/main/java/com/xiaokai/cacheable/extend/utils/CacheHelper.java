package com.xiaokai.cacheable.extend.utils;


import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.xiaokai.cacheable.annotation.ExpirableCacheable;
import com.xiaokai.cacheable.constant.CacheConstant;
import com.xiaokai.cacheable.extend.CustomizedRedisCacheManager;
import com.xiaokai.cacheable.extend.init.CacheExpireTimeInit;
import com.xiaokai.cacheable.extend.model.CacheMetaData;
import com.xiaokai.cacheable.extend.model.CachedInvocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.Map;

/**
 * 缓存过期时间以及过期时缓存自动刷新核心类
 */
@Slf4j
public final class CacheHelper {


    public static CustomizedRedisCacheManager getCacheManager() {
        return SpringUtil.getBean(CacheConstant.CUSTOM_CACHE_MANAGER, CustomizedRedisCacheManager.class);
    }


    /**
     * {@link CacheExpireTimeInit}
     *
     * @param expirableCacheable
     */
    public static void initExpireTime(ExpirableCacheable expirableCacheable) {
        // 获取缓存名称：通过 getCacheNames 方法从 ExpirableCacheable 注解中获取缓存名称。
        String[] cacheNames = getCacheNames(expirableCacheable);

        if (ArrayUtil.isNotEmpty(cacheNames)) {
            // 更新缓存配置
            // 遍历每个缓存名称，创建新的 RedisCacheConfiguration，设置其过期时间（使用注解中的 expiredTimeSecond）。将新的配置存入 initialCacheConfigurations 中。
            CustomizedRedisCacheManager customizedRedisCacheManager = getCacheManager();
            if(customizedRedisCacheManager == null){
                log.error("CustomizedRedisCacheManager is null");
                return;
            }
            Map<String, RedisCacheConfiguration> initialCacheConfigurations = customizedRedisCacheManager.getInitialCacheConfigurations();
            RedisCacheConfiguration defaultCacheConfiguration = customizedRedisCacheManager.getDefaultCacheConfiguration();
            for (String cacheName : cacheNames) {
                RedisCacheConfiguration redisCacheConfiguration = defaultCacheConfiguration.entryTtl(Duration.ofSeconds(expirableCacheable.expiredTimeSecond()));
                initialCacheConfigurations.put(cacheName, redisCacheConfiguration);
            }

        }
    }

    /**
     * 初始化自定义缓存
     */
    public static void initializeCaches() {
        getCacheManager().initializeCaches();
        log.info("初始化带有过期时间自定义Cacheable...");
        // log.info(JSON.toJSONString(getCacheManager().getInitialCacheConfigurations()));
    }


    /**
     * 获取缓存名称
     */
    public static String[] getCacheNames(ExpirableCacheable expirableCacheable) {
        String[] cacheNames = expirableCacheable.cacheNames();
        if (ArrayUtil.isEmpty(cacheNames)) {
            cacheNames = expirableCacheable.value();
        }
        return cacheNames;
    }

    /**
     * 刷新指定缓存名称的缓存内容
     * 缓存即将到期主动刷新缓存方法
     * @param cacheName 缓存名称
     */
    public static void refreshCache(String cacheName) {
        // 检查 cacheName 是否与当前的 CachedInvocation 对象中的缓存名称匹配，确保刷新操作应用到正确的缓存
        boolean isMatchCacheName = isMatchCacheName(cacheName);
        // 如果缓存名称匹配，则继续执行刷新缓存的逻辑
        if (isMatchCacheName) {
            // 从自定义的 RedisCacheManager 中获取当前保存的 CachedInvocation 对象
            // CachedInvocation 保存了方法调用相关的信息，例如方法的参数、结果等
            CachedInvocation cachedInvocation = getCacheManager().getCachedInvocation();
            boolean invocationSuccess; // 标识方法调用是否成功
            Object computed = null;
            try {
                computed = cachedInvocation.invoke();
                invocationSuccess = true;
            } catch (Exception ex) {
                invocationSuccess = false;
                log.error("更新缓存失败: {}", ex.getMessage());
            }

            if (invocationSuccess) {
                // 通过缓存管理器获取指定 cacheName 对应的 Cache 对象
                Cache cache = getCacheManager().getCache(cacheName);
                // 检查缓存是否为空，确保缓存对象存在
                if (ObjectUtil.isNotEmpty(cache)) {
                    // 从 CachedInvocation 的元数据中获取用于缓存的键（cacheKey）。该键用于标识缓存条目
                    Object cacheKey = cachedInvocation.getMetaData().getKey();
                    // 将计算出的新结果 computed 以 cacheKey 为键存入缓存
                    if (cache != null) {
                        cache.put(cacheKey, computed);
                    }
                    log.info("更新缓存成功，name: {}，key:{}", cacheName, cacheKey);
                }
            }
        }

    }

    /**
     * 检查指定的 cacheName 是否与 CachedInvocation 中的缓存名称匹配，确保操作作用于正确的缓存
     */
    private static boolean isMatchCacheName(String cacheName) {
        // 从自定义的缓存管理器中获取当前的 CachedInvocation 对象
        // CachedInvocation 存储了与缓存相关的方法调用信息
        CachedInvocation cachedInvocation = getCacheManager().getCachedInvocation();
        if (ObjectUtil.isEmpty(cachedInvocation)) {
            log.warn("CachedInvocation为空");
            return false;
        }
        // 从 cachedInvocation 对象中获取缓存元数据 metaData
        // 该对象包含与缓存相关的信息，例如缓存名称、缓存键等
        CacheMetaData metaData = cachedInvocation.getMetaData();
        // 遍历 metaData 中的所有缓存名称，检查它们是否与传入的 cacheName 相等
        // 如果找到了匹配的缓存名称，返回 true，表示名称匹配
        for (String name : metaData.getCacheNames())
            if (name.equals(cacheName))
                return true;
        return true;
    }
}
