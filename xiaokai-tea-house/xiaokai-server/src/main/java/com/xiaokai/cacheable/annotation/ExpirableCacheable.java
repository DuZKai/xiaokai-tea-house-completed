package com.xiaokai.cacheable.annotation;

import com.xiaokai.cacheable.constant.CacheConstant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/*
* 自定义注解，继承了Spring的@Cacheable注解，新增expiredTimeSecond缓存过期时间以及缓存自动刷新时间
* */

// 注解可以应用于类 (TYPE) 和方法 (METHOD) 上
@Target({ElementType.TYPE, ElementType.METHOD})
// 注解将在运行时可用（RUNTIME），这意味着可以通过反射读取它
@Retention(RetentionPolicy.RUNTIME)
// 允许子类继承该注解
@Inherited
// 生成文档时包含注解
@Documented
// 该自定义注解默认继承了 Spring 的 @Cacheable，指定了自定义的 cacheManager 和 keyGenerator
@Cacheable(cacheManager = CacheConstant.CUSTOM_CACHE_MANAGER,keyGenerator = CacheConstant.CUSTOM_CACHE_KEY_GENERATOR)
public @interface ExpirableCacheable {

    // 指定缓存的名称。如果不传，则使用默认值 []（空数组）
    @AliasFor(annotation = Cacheable.class,attribute = "value")
    String[] value() default {};

    // 指定缓存名称
    @AliasFor(annotation = Cacheable.class,attribute = "cacheNames")
    String[] cacheNames() default {};

    // key 属性的别名，指定缓存的键
    @AliasFor(annotation = Cacheable.class,attribute = "key")
    String key() default "";

    // 指定自定义的键生成器
    @AliasFor(annotation = Cacheable.class,attribute = "keyGenerator")
    String keyGenerator() default "";

    // 指定自定义缓存解析器
    @AliasFor(annotation = Cacheable.class,attribute = "cacheResolver")
    String cacheResolver() default "";

    // 指定缓存的条件表达式
    @AliasFor(annotation = Cacheable.class,attribute = "condition")
    String condition() default "";

    // 表示何时不缓存
    @AliasFor(annotation = Cacheable.class,attribute = "unless")
    String unless() default "";

    // 指定是否进行同步缓存
    @AliasFor(annotation = Cacheable.class,attribute = "sync")
    boolean sync() default false;

    // 指定缓存的过期时间，以秒为单位。默认为 0，表示不过期。
    long expiredTimeSecond() default -1;

    // 指定缓存即将到期自动刷新功能，以秒为单位。默认为 0，表示不预加载。
    long preLoadTimeSecond() default -1;


}
