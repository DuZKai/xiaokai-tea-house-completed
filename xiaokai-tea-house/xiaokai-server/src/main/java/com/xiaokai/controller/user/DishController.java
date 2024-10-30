package com.xiaokai.controller.user;

import com.xiaokai.cacheable.annotation.ExpirableCacheable;
import com.xiaokai.constant.StatusConstant;
import com.xiaokai.entity.Dish;
import com.xiaokai.result.Result;
import com.xiaokai.service.DishService;
import com.xiaokai.util.OssUtil;
import com.xiaokai.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.xiaokai.constant.RedisConstant.*;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private OssUtil ossUtil;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    // @Cacheable(cacheNames = DISH, key = "#categoryId")
    @ExpirableCacheable(cacheNames = DISH, key = "#categoryId", expiredTimeSecond = Expired_Time_Second, preLoadTimeSecond = Pre_Load_Time_Second)
    public Result<List<DishVO>> list(Long categoryId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);

        // 查询数据库
        List<DishVO> list = dishService.listWithFlavor(dish);

        for(DishVO dishVO : list){
            dishVO.setImage(ossUtil.getOssUrl(dishVO.getImage()));
        }

        return Result.success(list);
    }

}
