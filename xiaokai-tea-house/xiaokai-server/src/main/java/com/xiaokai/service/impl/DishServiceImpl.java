package com.xiaokai.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xiaokai.constant.MessageConstant;
import com.xiaokai.constant.StatusConstant;
import com.xiaokai.dto.DishDTO;
import com.xiaokai.dto.DishPageQueryDTO;
import com.xiaokai.entity.Dish;
import com.xiaokai.entity.DishFlavor;
import com.xiaokai.entity.Employee;
import com.xiaokai.entity.Setmeal;
import com.xiaokai.exception.DeletionNotAllowedException;
import com.xiaokai.mapper.DishFlavorMapper;
import com.xiaokai.mapper.DishMapper;
import com.xiaokai.mapper.SetmealDishMapper;
import com.xiaokai.mapper.SetmealMapper;
import com.xiaokai.result.PageResult;
import com.xiaokai.service.DishService;
import com.xiaokai.util.OssUtil;
import com.xiaokai.vo.DishVO;
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
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private OssUtil ossUtil;


    /**
     * 新增菜品
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavour(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 只保存图片名称
        dish.setImage(ossUtil.use_name_get_url(dish.getImage()));

        // 向菜品表插入一条数据
        dishMapper.insert(dish);

        // 获取插入的菜品id
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            // 向口味表插入N条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        for (DishVO dish : page.getResult()) {
            // 生成OSS真正URL
            dish.setImage(ossUtil.getOssUrl(dish.getImage()));
        }

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断菜品是否起售中
        for(Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断菜品是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(ids);
        if(setmealIds != null && !setmealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 删除菜品表中菜品数据
        // for(Long id : ids) {
        //     dishMapper.deleteById(id);
        //     // 删除口味表中菜品数据
        //     dishFlavorMapper.deleteByDishId(id);
        // }

        // 优化：批量删除菜品数据
        dishMapper.deleteByIds(ids);
        // 批量删除口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品和口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 生成OSS真正URL
        dish.setImage(ossUtil.getOssUrl(dish.getImage()));

        // 根据id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 封装数据
        DishVO dishVO = new DishVO();

        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id更新菜品和口味数据
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 只保存图片名称
        dish.setImage(ossUtil.use_name_get_url(dish.getImage()));

        // 修改菜品表基本信息
        dishMapper.update(dish);
        // 删除原有口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        // 插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 启用或者禁用菜品
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder().status(status).id(id).build();
        dishMapper.update(dish);

        // 判断菜品是否被套餐关联
        if(Objects.equals(status, StatusConstant.DISABLE)) {
            // 停售需要将包含菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(dishIds);
            if(setmealIds != null && !setmealIds.isEmpty()) {
                setmealIds.forEach(setmealId -> {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                });
            }
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> listByCategoryId(Long categoryId) {
        Dish dish = Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();

        List<Dish> dishList = dishMapper.listByCategoryId(dish);

        for(Dish d : dishList) {
            // 生成OSS真正URL
            d.setImage(ossUtil.getOssUrl(d.getImage()));
        }

        return dishList;
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.listByCategoryId(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);

            // 生成OSS真正URL
            d.setImage(ossUtil.getOssUrl(d.getImage()));
        }

        return dishVOList;
    }

}
