package com.example.medicinebox.security;

import com.example.medicinebox.entity.User;
import com.example.medicinebox.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 自定义用户详情服务
 *
 * <p>实现Spring Security的UserDetailsService接口，
 * 用于根据手机号从数据库中加载用户信息，供Spring Security认证使用。
 *
 * <p>主要职责：
 * <ul>
 *   <li>根据手机号查询用户</li>
 *   <li>将用户实体转换为Spring Security的UserDetails对象</li>
 *   <li>设置用户的角色权限（ROLE_前缀）</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /** 用户数据访问层，用于查询用户信息 */
    private final UserRepository userRepository;

    /**
     * 构造函数，注入用户仓库
     *
     * @param userRepository 用户数据访问层
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 根据手机号加载用户详情
     *
     * <p>Spring Security在进行认证时会调用此方法获取用户信息。
     *
     * <p>处理逻辑：
     * <ol>
     *   <li>根据手机号从数据库查询用户</li>
     *   <li>如果用户不存在，抛出UsernameNotFoundException</li>
     *   <li>获取用户角色，默认为"USER"</li>
     *   <li>构建Spring Security的User对象，包含手机号、密码和权限列表</li>
     *   <li>角色前缀添加"ROLE_"以符合Spring Security的角色判断规范</li>
     * </ol>
     *
     * @param phone 用户手机号（作为登录用户名）
     * @return UserDetails Spring Security用户详情对象
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // 根据手机号查询用户，不存在则抛出异常
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + phone));

        // 获取用户角色，如果为空则默认为USER
        String role = user.getRole() != null ? user.getRole() : "USER";
        // 构建Spring Security的User对象
        // 包含：用户名（手机号）、密码、授权列表（角色）
        return new org.springframework.security.core.userdetails.User(
                user.getPhone(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
