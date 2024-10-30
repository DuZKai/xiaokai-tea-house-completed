package com.xiaokai.cacheable.extend.aspect;


import cn.hutool.extra.spring.SpringUtil;
import com.xiaokai.cacheable.annotation.ExpirableCacheable;
import com.xiaokai.cacheable.constant.CacheConstant;
import com.xiaokai.cacheable.extend.model.CacheMetaData;
import com.xiaokai.cacheable.extend.model.CachedInvocation;
import com.xiaokai.cacheable.extend.utils.CacheHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

import static com.xiaokai.cacheable.extend.utils.CacheHelper.getCacheNames;

/**
 * 获取即将到期时间参数切面，并进行事件发布调用对象CachedInvocation
 */
@Component
// AOP 切面，意味着这个类会对某些方法进行拦截，并在方法执行前后执行相应的逻辑。
@Aspect
@Slf4j
// 必须设置order(K)，k只需为任意即可，因为需要保证先执行这段方法再执行CustomizedRedisCache的get方法
@Order(100)
public class ExpirableCacheablePreLoadAspect {

    @Autowired
    private ApplicationContext applicationContext; // 用于后续发布事件（即缓存预加载的元数据信息）


    @SneakyThrows
    // @Around：标记了这是一个环绕通知，@Around 会拦截标注了 @ExpirableCacheable 注解的方法
    // 切入点表达式，表示当方法上带有 @ExpirableCacheable 注解时会触发此切面
    @Around(value = "@annotation(expirableCacheable)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, ExpirableCacheable expirableCacheable) {
        // 在目标方法执行之前，调用此方法构建缓存调用的信息，并通过事件发布机制通知其他组件（通常是缓存管理器）进行缓存初始化或预加载。
        buildCachedInvocationAndPushlish(proceedingJoinPoint, expirableCacheable);
        // 继续执行目标方法，并将其结果返回。这是 AOP 的关键，控制目标方法的执行流程
        return proceedingJoinPoint.proceed();
    }

    /**
     * 缓存调用元数据构建
     * */
    private void buildCachedInvocationAndPushlish(ProceedingJoinPoint proceedingJoinPoint, ExpirableCacheable expirableCacheable) {
        // 通过反射获取目标方法的具体实现（包括接口、桥接方法等）
        Method method = this.getSpecificmethod(proceedingJoinPoint);
        // 获取 @ExpirableCacheable 注解中定义的缓存名称
        String[] cacheNames = getCacheNames(expirableCacheable);
        // 获取目标对象，即被 @ExpirableCacheable 注解的类实例
        Object targetBean = proceedingJoinPoint.getTarget();
        // 获取目标方法的参数，用于缓存键的生成和方法调用
        Object[] arguments = proceedingJoinPoint.getArgs();
        // 通过自定义的 KeyGenerator 来生成缓存键，根据目标类、方法、参数生成唯一的缓存键
        KeyGenerator keyGenerator = SpringUtil.getBean(CacheConstant.CUSTOM_CACHE_KEY_GENERATOR, KeyGenerator.class);
        Object key = keyGenerator.generate(targetBean, method, arguments);
        // 构建缓存调用的元数据信息，包含了目标方法、参数、缓存名称、缓存键、过期时间和预加载时间等
        // 通过@Builder模式创建 CachedInvocation 对象
        CachedInvocation cachedInvocation = CachedInvocation.builder()
                .arguments(arguments)
                .targetBean(targetBean)
                .targetMethod(method)
                .metaData(CacheMetaData.builder()
                        .cacheNames(cacheNames)
                        .key(key)
                        .expiredTimeSecond(expirableCacheable.expiredTimeSecond())
                        .preLoadTimeSecond(expirableCacheable.preLoadTimeSecond())
                        .build()
                )
                .build();
        // 发布缓存调用元数据信息事件，使得其他组件（如缓存管理器）可以监听到这个事件并执行相应的预加载操作
        applicationContext.publishEvent(cachedInvocation);
    }

    private Method getSpecificmethod(ProceedingJoinPoint pjp) {
        // 从 ProceedingJoinPoint 中提取方法签名，以获取目标方法的信息
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();

        // 获取代理对象的真实类，这在 AOP 代理类中尤为重要，因为代理类可能是被增强的对象，而非实际的目标对象。
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pjp.getTarget());
        // 该方法可能位于接口上，但我们需要目标类的属性。如果目标类为null，则方法将保持不变。
        if (targetClass == null && pjp.getTarget() != null) {
            targetClass = pjp.getTarget().getClass();
        }
        // 获取最具体的方法，确保在多态的情况下，找到正确的重载方法。
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        // 处理泛型桥接方法的问题。在 Java 中，泛型类型擦除会导致桥接方法，反射时需要找到原始的桥接方法
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        return specificMethod;
    }

}
