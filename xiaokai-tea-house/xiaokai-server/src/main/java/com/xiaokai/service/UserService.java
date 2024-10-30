package com.xiaokai.service;


import com.xiaokai.dto.UserLoginDTO;
import com.xiaokai.entity.User;

public interface UserService {
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
     User wxLogin(UserLoginDTO userLoginDTO);
}
