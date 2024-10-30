package com.xiaokai.controller.user;

import com.xiaokai.constant.JwtClaimsConstant;
import com.xiaokai.dto.UserLoginDTO;
import com.xiaokai.entity.User;
import com.xiaokai.mapper.UserMapper;
import com.xiaokai.properties.JwtProperties;
import com.xiaokai.result.Result;
import com.xiaokai.service.UserService;
import com.xiaokai.utils.JwtUtil;
import com.xiaokai.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Api(tags = "C端用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userSerive;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 微信登录
     *
     * */
    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信登录:{}", userLoginDTO.getCode());

        // 微信登录
        User user = userSerive.wxLogin(userLoginDTO);

        // 为我微信用户生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder().id(user.getId()).openid(user.getOpenid()).token(token).build();
        return Result.success(userLoginVO);
    }
}
