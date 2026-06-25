package com.cgrs.driver.dao;
import com.cgrs.driver.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/*写注册功能时创建，与注册和登录功能相关
作用：JPA 自动生成数据库操作代码，定义接口方法名。

existsByUsername(String username) →
自动生成 SQL：SELECT COUNT(*) FROM user WHERE username = ?

save(User user) → 自动生成 SQL：INSERT INTO user (...) VALUES (...)

继承 JpaRepository<User, Long> 后还免费获得 findById、findAll 等方法。*/
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 根据用户名或邮箱查找用户
    @Query("SELECT u FROM User u WHERE u.username = :account OR u.email = :account")
    Optional<User> findByUsernameOrEmail(@Param("account") String account);

    // 根据邮箱查找用户
    Optional<User> findByEmail(String email);
    
    // 根据角色查找用户列表
    java.util.List<User> findByRole(Integer role);
}
