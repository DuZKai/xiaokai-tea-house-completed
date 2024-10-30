package com.xiaokai.service.impl;

import com.xiaokai.context.BaseContext;
import com.xiaokai.dto.ShoppingCartDTO;
import com.xiaokai.entity.Dish;
import com.xiaokai.entity.DishFlavor;
import com.xiaokai.entity.Setmeal;
import com.xiaokai.entity.ShoppingCart;
import com.xiaokai.mapper.DishMapper;
import com.xiaokai.mapper.SetmealMapper;
import com.xiaokai.mapper.ShoppingCartMapper;
import com.xiaokai.util.OssUtil;
import com.xiaokai.service.ShoppingCartService;
import com.xiaokai.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private OssUtil ossUtil;

    /**
     * 添加购物车
     * */
    public void addShoppingCart(ShoppingCartDTO shoppingcartDTO) {
        // 判断当前加入到购物车中商品是否已经存在
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingcartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 如果存在。只需将数量加一
        if(Objects.nonNull(list) && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }
        else{
            // 如果不存在，插入购物车数据
            // 判断本次添加到购物车的是菜品还是套餐
            Long dishId = shoppingcartDTO.getDishId();
            if(Objects.nonNull(dishId)){
                // 添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            }

            Long setmealId = shoppingcartDTO.getSetmealId();
            if(Objects.nonNull(setmealId)){
                // 添加到购物车的是套餐
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }

            if(Objects.isNull(dishId) && Objects.isNull(setmealId)){
                log.error("添加购物车失败，菜品id和套餐id不能同时为空");
                log.error(shoppingcartDTO.toString());
                return;
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> showShoppingCart() {
        ShoppingCart shoppingCart = ShoppingCart. builder(). userId(BaseContext.getCurrentId()). build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 生成OSS真正URL
        for (ShoppingCart sc : list) {
            sc.setImage(ossUtil.getOssUrl(sc.getImage()));
        }

        return list;
    }

    /**
     * 清空购物车商品
     */
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

}
