package com.lnu.service;

import com.lnu.entity.User;
import java.util.List;

public interface UserService {

    /**
     * 用户登录
     * @param username 学号/工号
     * @param password 密码
     * @return 登录成功的用户信息（包含 token）
     */
    User login(String username, String password);

    /**
     * 获取用户列表 (管理端)
     * @param roleType 角色类型 'student' | 'teacher'
     * @return 用户列表
     */
    List<User> getList(String roleType);

    /**
     * 新增或更新用户 (管理端)
     * @param user 用户实体
     */
    void saveUser(User user);

    /**
     * 重置密码 (管理端)
     * @param id 用户ID
     */
    void resetPassword(Long id);
}