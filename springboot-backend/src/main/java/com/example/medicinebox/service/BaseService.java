package com.example.medicinebox.service;

import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 服务层基类
 * 提供公共方法供子类继承使用，主要功能是获取当前登录用户
 */
public abstract class BaseService {

    protected final UserRepository userRepository;

    public BaseService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取当前登录用户
     * 从Spring Security上下文中获取用户认证信息，再根据手机号查询用户实体
     *
     * @return 当前登录用户实体
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    protected User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}