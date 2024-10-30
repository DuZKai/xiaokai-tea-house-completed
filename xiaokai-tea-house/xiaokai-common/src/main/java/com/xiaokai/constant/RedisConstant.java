package com.xiaokai.constant;

/**
 * 状态常量，启用或者禁用
 */
public class RedisConstant {

    // 用户登录Redis过期时间
    public static final Integer LOGIN_USER_TTL = 5;

    // 数据缓存过期时间
    public static final int Expired_Time_Second = 5 * 60;

    // 数据缓存刷新时间
    public static final int Pre_Load_Time_Second = 4 * 60;

    // 菜品保存名,规则：dish_分类id
    public static final String DISH = "dish";

    // 套餐保存名,规则：setmeal_分类id
    public static final String SETMEAL = "setmeal";

}
