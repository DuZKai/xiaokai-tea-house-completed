package com.xiaokai.cacheable.autoconfigure;


import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xiaokai.cacheable.extend.CustomizedRedisCacheManager;
import com.xiaokai.cacheable.constant.CacheConstant;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.xiaokai.cacheable.constant.CacheConstant.REDISTEMPLATE_BEAN_NAME;

// 使用 @Configuration 注解标记该类为配置类，表示该类包含 Spring 的 Bean 定义。
@Configuration
// 使用 @Import(SpringUtil.class) 注解导入 SpringUtil 类，以便在此配置中使用其 Bean。
@Import(SpringUtil.class)
// 使用 @ComponentScan(basePackages = "com.xiaokai.cacheable") 注解指定要扫描的包，以便 Spring 可以找到并注册该包下的组件。
@ComponentScan(basePackages = "com.xiaokai.cacheable")
// 使用 @EnableCaching 注解启用 Spring 的缓存支持。
@EnableCaching
public class CustomizedRedisAutoConfiguration {

    // 使用 @Bean 注解定义一个名为 REDISTEMPLATE_BEAN_NAME 的 Bean，返回类型为 RedisTemplate<String, Object>。
    @Bean(REDISTEMPLATE_BEAN_NAME)
    // cacheRedisTemplate 方法接受一个 RedisConnectionFactory 参数，用于创建 Redis 连接
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建一个新的 RedisTemplate 实例，并设置其连接工厂。
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer());
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer());
        // 完成属性设置：调用 afterPropertiesSet 方法，以确保所有属性都已设置。
        template.afterPropertiesSet();
        return template;
    }

    // 将默认的缓存管理器改成我们自定义的缓存管理器
    @Bean(CacheConstant.CUSTOM_CACHE_MANAGER)
    public CustomizedRedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisTemplate cacheRedisTemplate) {
        // 创建一个非锁定的 Redis 缓存写入器。
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
        // 创建默认缓存配置：使用 RedisCacheConfiguration.defaultCacheConfig() 创建默认配置，并设置缓存条目的过期时间为30分钟
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30L));

        defaultCacheConfig = defaultCacheConfig
                // 设置key采用String的序列化方式
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
                //设置value序列化方式采用jackson方式序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer()))
                //当value为null时不进行缓存
                .disableCachingNullValues();

        // 初始化缓存配置映射：创建一个空的 Map 用于存储初始的缓存配置
        Map<String, RedisCacheConfiguration> initialCacheConfiguration = new HashMap<>();
        // 创建并返回 CustomizedRedisCacheManager 的实例，使用之前创建的 RedisCacheWriter、默认缓存配置、初始缓存配置和 RedisTemplate
        return new CustomizedRedisCacheManager(redisCacheWriter, defaultCacheConfig, initialCacheConfiguration, cacheRedisTemplate);
    }

    // 生成缓存键的简单生成器
    @Bean(CacheConstant.CUSTOM_CACHE_KEY_GENERATOR)
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    private Jackson2JsonRedisSerializer jackson2JsonRedisSerializer() {
        // 创建一个 Jackson2JsonRedisSerializer 实例，用于序列化和反序列化 Redis 中的对象。
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        // 创建一个 ObjectMapper 实例，用于配置序列化行为。
        ObjectMapper om = new ObjectMapper();
        // 设置 ObjectMapper 的可见性，以便所有字段都可以被序列化。
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启用默认类型支持，允许对非最终类进行类型识别。
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        // 将配置好的 ObjectMapper 设置到 Jackson2JsonRedisSerializer。
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

    private GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()).setTimeZone(TimeZone.getDefault());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
