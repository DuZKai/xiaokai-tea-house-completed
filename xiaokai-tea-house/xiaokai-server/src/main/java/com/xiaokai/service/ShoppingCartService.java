package com.xiaokai.service;

import com.xiaokai.dto.ShoppingCartDTO;
import com.xiaokai.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    /**
     * 添加购物车
     * @param shoppingcartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingcartDTO);

    /**
     * 查看购物车
     * @return
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车商品
     */
    void cleanShoppingCart();
}
