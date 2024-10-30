package com.xiaokai.controller.admin;

import com.xiaokai.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Objects;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    // @Autowired
    // private RedisTemplate redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置店铺营业状态
     *
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态: {}", status == 1 ? "营业中" : "打烊中");
        stringRedisTemplate.opsForValue().set(KEY, String.valueOf(status));
        // redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    /**
     * 获取店铺营业状态
     *
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus(){
        String s_status = stringRedisTemplate.opsForValue().get(KEY);
        int status = 0;
        if (s_status != null)
            status = Integer.parseInt(s_status);
        // Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        // if (Objects.isNull(status)) {
        //     status = 0;
        // }
        log.info("获取店铺营业状态: {}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
