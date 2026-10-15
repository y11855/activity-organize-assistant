package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.UserMapper;
import com.aiapp.activityassistant.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户业务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    public List<User> listAll() {
        List<User> list = list(new LambdaQueryWrapper<User>().orderByAsc(User::getId));
        list.forEach(u -> u.setPassword(null));
        return list;
    }
}
