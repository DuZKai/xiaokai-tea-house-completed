package com.xiaokai.cacheable.extend.init;


import cn.hutool.core.map.MapUtil;
import com.xiaokai.cacheable.annotation.ExpirableCacheable;
import com.xiaokai.cacheable.extend.utils.CacheHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Map;

/*
* 类实现了 SmartInitializingSingleton 和 BeanFactoryAware 接口。
* SmartInitializingSingleton：在所有单例 Bean 实例化后执行特定逻辑。
* BeanFactoryAware：允许类访问 Spring 的 BeanFactory。
* */
// 将该类标记为 Spring 组件，以便自动检测和注册为 Spring Bean
@Component
@Slf4j
public class CacheExpireTimeInit implements SmartInitializingSingleton, BeanFactoryAware {
    
    private DefaultListableBeanFactory beanFactory;

    /* 实现 BeanFactoryAware 接口的方法，将传入的 BeanFactory 强制转换为 DefaultListableBeanFactory 并保存
    * @param beanFactory BeanFactory 对象
    * @throws BeansException
    * @return
    * */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory)beanFactory;
    }

    // 重写 SmartInitializingSingleton 接口的方法，该方法在所有单例 Bean 实例化后调用。
    @Override
    public void afterSingletonsInstantiated() {
        // 获取带有 @Component 注解的 Bean：调用 getBeansWithAnnotation 方法获取所有被 @Component 注解标记的 Bean，并将它们存储在 beansWithAnnotation 映射中
        Map<String, Object> beansWithAnnotation = beanFactory.getBeansWithAnnotation(Component.class);
        // 检查 Bean 映射是否为空：使用 MapUtil.isNotEmpty 方法判断 beansWithAnnotation 是否非空。
        if(MapUtil.isNotEmpty(beansWithAnnotation)){
            for (Object cacheValue : beansWithAnnotation.values()) {
                // 反射调用 doWithMethods 方法：对当前 Bean 的所有方法进行遍历。
                ReflectionUtils.doWithMethods(cacheValue.getClass(), method -> {
                    // 只有非"finalize"（Java 的垃圾回收方法）、非私有和非本地方法会被处理。
                    if (!method.getName().equals("finalize") &&
                        !Modifier.isPrivate(method.getModifiers()) &&
                        !Modifier.isNative(method.getModifiers())) {
                        // 将方法的访问权限设置为可访问，以便可以调用。
                        ReflectionUtils.makeAccessible(method);
                        // 使用 isAnnotationPresent 方法检查当前方法是否带有 ExpirableCacheable 注解。
                        boolean cacheAnnotationPresent = method.isAnnotationPresent(ExpirableCacheable.class);
                        if (cacheAnnotationPresent) {
                            // 获取注解实例：通过 getAnnotation 方法获取 ExpirableCacheable 注解实例
                            ExpirableCacheable expirableCacheable = method.getAnnotation(ExpirableCacheable.class);
                            // 初始化过期时间：调用 CacheHelper.initExpireTime 方法，传入 expirableCacheable 注解，进行过期时间的初始化。
                            CacheHelper.initExpireTime(expirableCacheable);
                        }
                    }
                });
            }
            // 初始化缓存：调用 CacheHelper.initializeCaches 方法
            // 在CacheHelper再次初始化initializeCaches，官方文档表明可以在运行时重新初始化缓存，这里重新初始化缓存主要为了设置过期时间。
            CacheHelper.initializeCaches();
        }
    }

}
