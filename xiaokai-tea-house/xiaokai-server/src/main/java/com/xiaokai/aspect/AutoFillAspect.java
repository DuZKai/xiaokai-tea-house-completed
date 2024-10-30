package com.xiaokai.aspect;

import com.xiaokai.annotation.AutoFill;
import com.xiaokai.constant.AutoFillConstant;
import com.xiaokai.context.BaseContext;
import com.xiaokai.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.xiaokai.mapper.*.*(..)) && @annotation(com.xiaokai.annotation.AutoFill)")
    public void autoFillPointCut() {

    }

    /**
     * 前置通知，在通知中进行公共字段赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始公共字段自动填充处理...");
        // 获取当前被拦截方法上数据库操作类型
        // 获取当前被拦截方法的签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获得当前被拦截方法上的注解
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        // 获取当前被拦截方法上的操作类型
        OperationType operationType = autoFill.value();
        // 获取当前被拦截方法的参数（实体对象）
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];

        // 准备赋值数据
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = BaseContext.getCurrentId();

        // 根据当前不同操作类型，为对应属性进行反射进行赋值
        switch (operationType) {
            case INSERT: {
                try {
                    // invoke通过反射未对象属性赋值
                    entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class).invoke(entity, currentUserId);
                    entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentUserId);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            case UPDATE: {
                try {
                    entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentUserId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
