package com.xiaokai.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xiaokai.constant.MessageConstant;
import com.xiaokai.constant.StatusConstant;
import com.xiaokai.dto.DishDTO;
import com.xiaokai.dto.DishPageQueryDTO;
import com.xiaokai.dto.SetmealDTO;
import com.xiaokai.dto.SetmealPageQueryDTO;
import com.xiaokai.entity.Dish;
import com.xiaokai.entity.DishFlavor;
import com.xiaokai.entity.Setmeal;
import com.xiaokai.entity.SetmealDish;
import com.xiaokai.exception.DeletionNotAllowedException;
import com.xiaokai.exception.SetmealEnableFailedException;
import com.xiaokai.mapper.DishFlavorMapper;
import com.xiaokai.mapper.DishMapper;
import com.xiaokai.mapper.SetmealDishMapper;
import com.xiaokai.mapper.SetmealMapper;
import com.xiaokai.result.PageResult;
import com.xiaokai.service.DishService;
import com.xiaokai.util.OssUtil;
import com.xiaokai.service.SetmealService;
import com.xiaokai.vo.DishItemVO;
import com.xiaokai.vo.DishVO;
import com.xiaokai.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private OssUtil ossUtil;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     *
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 只保存图片名称
        setmeal.setImage(ossUtil.use_name_get_url(setmeal.getImage()));

        // 向套餐表插入一条数据
        setmealMapper.insert(setmeal);

        // 获取插入的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        // 保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<DishVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        for (DishVO dish : page.getResult()) {
            // 生成OSS真正URL
            dish.setImage(ossUtil.getOssUrl(dish.getImage()));
        }

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐批量删除
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断套餐是否起售中
        for (Long id : ids) {
            Setmeal dish = setmealMapper.getById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        // 批量删除套餐数据
        setmealMapper.deleteByIds(ids);
        // 批量删除套餐内菜品数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据id查询套餐和菜品数据
     *
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        // 根据id查询套餐数据
        Setmeal setmeal = setmealMapper.getById(id);

        // 根据id查询菜品数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getByDishId(id);

        // 封装数据
        SetmealVO setmealVO = new SetmealVO();

        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        // 生成OSS真正URL
        setmealVO.setImage(ossUtil.getOssUrl(setmeal.getImage()));

        return setmealVO;
    }

    /**
     * 根据id更新套餐和菜品数据
     *
     * @param setmealDTO
     */
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 只保存图片名称
        setmeal.setImage(ossUtil.use_name_get_url(setmeal.getImage()));

        // 修改套餐表基本信息
        setmealMapper.update(setmeal);

        //套餐id
        Long setmealId = setmealDTO.getId();

        // 删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        setmealDishMapper.deleteBySetmealId(setmealId);

        // 插入新的菜品数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        // 重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 启用或者禁用套餐
     *
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if (status == StatusConstant.ENABLE) {
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if (dishList != null && !dishList.isEmpty()) {
                dishList.forEach(dish -> {
                    if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        Setmeal setmeal = Setmeal.builder().status(status).id(id).build();
        setmealMapper.update(setmeal);
    }


    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        List<DishItemVO> dishItemVOList = setmealMapper.getDishItemBySetmealId(id);
        for(DishItemVO dishItemVO : dishItemVOList) {
            // 生成OSS真正URL
            dishItemVO.setImage(ossUtil.getOssUrl(dishItemVO.getImage()));
        }
        return dishItemVOList;
    }
}
