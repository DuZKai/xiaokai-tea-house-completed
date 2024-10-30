package com.xiaokai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaokai.constant.MessageConstant;
import com.xiaokai.dto.UserLoginDTO;
import com.xiaokai.entity.User;
import com.xiaokai.exception.LoginFailedException;
import com.xiaokai.mapper.UserMapper;
import com.xiaokai.properties.WeChatProperties;
import com.xiaokai.service.UserService;
import com.xiaokai.utils.HttpClientUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private WeChatProperties weChatProperties;

    // 微信接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    // 微信请求属性
    public static final String APPID = "appid";
    public static final String SECRET = "secret";
    public static final String JSCODE = "js_code";
    public static final String GRANT_TYPE = "grant_type";
    public static final String OPENID = "openid";
    @Autowired
    private UserMapper userMapper;

    private String getOpenid(String code){
        // 调用微信接口服务，获得当前微信用户openid
        // 调用微信接口服务，获得当前微信用户openid
        Map<String, String> map = new HashMap<>();
        map.put(APPID, weChatProperties.getAppid());
        map.put(SECRET, weChatProperties.getSecret());
        map.put(JSCODE, code);
        map.put(GRANT_TYPE, "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString(OPENID);
    }

    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());
        // 判断openid是否为空，如果为空表示登录失败，抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 判断当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);

        // 如果是新用户，自动完成注册
        if(user == null){
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();
            userMapper.insert(user);
        }
        // 返回这个用户对象
        return user;
    }
}
