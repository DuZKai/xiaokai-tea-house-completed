package com.xiaokai.cacheable.extend.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;


/**
 * @description: 标记了缓存注解的方法类信息,用于主动刷新缓存时调用原始方法加载数据
 * 封装缓存注解对象CachedInvocation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
// 自动生成构建器模式
@Builder
public final class CachedInvocation {

    private CacheMetaData metaData;
    private Object targetBean;
    private Method targetMethod;
    private Object[] arguments;


    /**
     * 通过反射调用先前存储的 targetMethod，并返回该方法的执行结果
     * @return Object
     * */
    public Object invoke()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // MethodInvoker 是 Spring 中的一个实用类，简化了反射调用的方法调用过程
        // 它通过提供目标对象、方法名称和参数，来封装反射调用的细节
        final MethodInvoker invoker = new MethodInvoker();
        // 将目标对象 (targetBean) 设置给 invoker，表示反射调用将发生在这个对象上。
        invoker.setTargetObject(this.getTargetBean());
        // 将存储在 CachedInvocation 中的参数 (arguments) 传递给 invoker，用于调用方法时使用
        invoker.setArguments(this.getArguments());
        // 通过 targetMethod.getName() 提取方法名称，传递给 invoker，以便它知道调用哪个方法
        invoker.setTargetMethod(this.getTargetMethod().getName());
        // 调用 invoker.prepare() 方法，这一步是 MethodInvoker 提供的，用于准备反射调用
        // 它会检查方法和参数是否正确设置
        invoker.prepare();
        // 调用 invoker.invoke()，实际上通过反射调用 targetBean 上的 targetMethod
        // 并传递必要的参数。该方法返回调用结果，作为整个 invoke 方法的返回值
        return invoker.invoke();
    }


}

