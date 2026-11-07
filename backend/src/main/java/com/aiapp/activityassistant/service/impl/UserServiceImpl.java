package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.UserMapper;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.UserSimpleVO;
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
    public User getByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1"));
    }

    @Override
    public List<UserSimpleVO> listAll() {
        // 仅返回启用账号，禁用账号不再出现在成员选择器
        return list(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId)).stream().map(u -> {
                    UserSimpleVO vo = new UserSimpleVO();
                    vo.setId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setNickname(u.getNickname());
                    vo.setStatus(u.getStatus());
                    return vo;
                }).toList();
    }
}
