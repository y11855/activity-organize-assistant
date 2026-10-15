package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户业务接口
 */
public interface UserService extends IService<User> {

    /**
     * 按用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 查询全部用户（成员选择器用，不含密码）
     */
    List<User> listAll();
}
