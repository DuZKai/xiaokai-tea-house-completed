package com.xiaokai.cacheable.extend;


import com.xiaokai.cacheable.extend.model.CachedInvocation;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
 * 自定义缓存管理器并继承RedisCacheManager并重写loadCaches和createRedisCache方法
 * 继承自 RedisCacheManager 并实现了 BeanFactoryAware 接口，
 * 主要是为了获取 BeanFactory 对象，以便在需要时获取其他 Bean 对象。
 * */
public class CustomizedRedisCacheManager extends RedisCacheManager implements BeanFactoryAware {

    @Getter
    private Map<String, RedisCacheConfiguration> initialCacheConfigurations; // 保存初始缓存配置的映射（缓存名 -> 缓存配置）

    @Getter
    private RedisTemplate cacheRedisTemplate; // 存储 RedisTemplate 实例，用于与 Redis 进行操作

    private RedisCacheWriter cacheWriter; // 负责与 Redis 进行低级缓存读写操作的对象

    private DefaultListableBeanFactory beanFactory;

    @Getter
    private RedisCacheConfiguration defaultCacheConfiguration; // 存储 Redis 的默认缓存配置

    @Getter
    protected CachedInvocation cachedInvocation; // 保存缓存的调用信息

    /*
     * 构造函数
     * @param cacheWriter Redis 缓存写入器
     * @param defaultCacheConfiguration 默认缓存配置
     * @param initialCacheConfigurations 初始缓存配置
     * @param cacheRedisTemplate RedisTemplate 实例
     * @return
     * */
    public CustomizedRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, Map<String, RedisCacheConfiguration> initialCacheConfigurations, RedisTemplate cacheRedisTemplate) {
        // 调用父类的构造函数
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations);
        // 传入 Redis 缓存写入器、默认缓存配置、初始缓存配置等参数
        this.cacheWriter = cacheWriter;
        this.defaultCacheConfiguration = defaultCacheConfiguration;
        this.initialCacheConfigurations = initialCacheConfigurations;
        this.cacheRedisTemplate = cacheRedisTemplate;
    }

    /*
     * 重写父类方法，用于加载所有缓存。创建一个 LinkedList 来保存 RedisCache 对象
     * @return Collection<RedisCache> RedisCache 对象集合
     * */
    @Override
    protected Collection<RedisCache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();

        // 遍历缓存配置：遍历 initialCacheConfigurations，为每一个缓存名称和配置创建一个 RedisCache 实例，并将其添加到缓存列表中。
        for (Map.Entry<String, RedisCacheConfiguration> entry : getInitialCacheConfigurations().entrySet()) {
            caches.add(createRedisCache(entry.getKey(), entry.getValue()));
        }
        return caches;
    }

    /*
     * createRedisCache 方法：重写了父类的方法，用于创建 RedisCache 对象。
     * 根据传入的 cacheConfig，如果为空则使用默认的 defaultCacheConfiguration。
     * 创建了一个 CustomizedRedisCache 对象（可能是自定义的 RedisCache 实现），并返回该对象。
     * @param name 缓存名称
     * @param cacheConfig 缓存配置
     * @return RedisCache RedisCache 对象
     * */
    @Override
    public RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        return new CustomizedRedisCache(name, cacheWriter, cacheConfig != null ? cacheConfig : defaultCacheConfiguration);
    }


    /*
     * 实现 BeanFactoryAware 接口中的方法，将 BeanFactory 设置为 DefaultListableBeanFactory
     * 使得该类可以访问 Spring 的 Bean
     * @param beanFactory BeanFactory 对象
     * @return
     * */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }


    /*
     * 事件监听器方法 doWithCachedInvocationEvent
     * 当事件被触发时，该方法会被调用，并将 cachedInvocation 存储在类的 cachedInvocation 字段中。
     * @param cachedInvocation CachedInvocation 对象
     * */
    // 使用了另一种方式来监听事件（例如 Guava 事件总线）
    // @Subscribe
    // 使用 @EventListener 注解来监听特定事件类型 CachedInvocation。
    @EventListener
    private void doWithCachedInvocationEvent(CachedInvocation cachedInvocation) {
        this.cachedInvocation = cachedInvocation;
    }
}