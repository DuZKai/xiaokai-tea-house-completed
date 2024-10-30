package com.xiaokai.controller.admin;

import com.xiaokai.dto.DishDTO;
import com.xiaokai.dto.DishPageQueryDTO;
import com.xiaokai.dto.SetmealDTO;
import com.xiaokai.dto.SetmealPageQueryDTO;
import com.xiaokai.entity.Dish;
import com.xiaokai.result.PageResult;
import com.xiaokai.result.Result;
import com.xiaokai.service.DishService;
import com.xiaokai.service.SetmealService;
import com.xiaokai.vo.DishVO;
import com.xiaokai.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.xiaokai.constant.RedisConstant.SETMEAL;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐管理")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = SETMEAL, key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐: {}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询: {}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 套餐批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("套餐批量删除")
    @CacheEvict(cacheNames = SETMEAL, allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("套餐批量删除: {}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐: {}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 更新套餐
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = SETMEAL, allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐: {}", setmealDTO);
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 更新套餐状态
     * @param status
     * @param id
     * @return
     */
    @ApiOperation("启用或停用套餐")
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = SETMEAL, allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("更新套餐状态: {}, {}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

}
