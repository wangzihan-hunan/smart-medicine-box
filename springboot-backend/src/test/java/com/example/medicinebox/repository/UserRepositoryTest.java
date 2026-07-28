package com.example.medicinebox.repository;

import com.example.medicinebox.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("UserRepository 集成测试")
@Disabled("集成测试需要数据库环境,暂时禁用")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPhone("13800138000");
        testUser.setPassword("encodedPassword");
        testUser.setName("张三");
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setPhoneVerified(false);
        testUser.setLoginAttempts(0);
    }

    @Test
    @DisplayName("根据手机号查询 - 存在返回用户")
    void findByPhone_Exists_ReturnsUser() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByPhone("13800138000");

        assertTrue(found.isPresent());
        assertEquals("张三", found.get().getName());
        assertEquals("USER", found.get().getRole());
    }

    @Test
    @DisplayName("根据手机号查询 - 不存在返回空")
    void findByPhone_NotExists_ReturnsEmpty() {
        Optional<User> found = userRepository.findByPhone("13900139000");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("检查手机号是否存在 - 存在返回true")
    void existsByPhone_Exists_ReturnsTrue() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByPhone("13800138000");

        assertTrue(exists);
    }

    @Test
    @DisplayName("检查手机号是否存在 - 不存在返回false")
    void existsByPhone_NotExists_ReturnsFalse() {
        boolean exists = userRepository.existsByPhone("13900139000");

        assertFalse(exists);
    }

    @Test
    @DisplayName("保存用户 - 成功保存")
    void save_User_Success() {
        User saved = userRepository.save(testUser);

        assertNotNull(saved.getId());
        assertEquals("13800138000", saved.getPhone());
        assertEquals("ACTIVE", saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("更新用户 - 成功更新")
    void save_UpdateUser_Success() {
        User saved = entityManager.persistAndFlush(testUser);
        
        saved.setName("李四");
        saved.setPhoneVerified(true);
        User updated = userRepository.save(saved);

        assertEquals("李四", updated.getName());
        assertTrue(updated.getPhoneVerified());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    @DisplayName("删除用户 - 成功删除")
    void delete_User_Success() {
        User saved = entityManager.persistAndFlush(testUser);
        
        userRepository.delete(saved);
        entityManager.flush();

        assertFalse(userRepository.existsById(saved.getId()));
    }
}