package com.lnu.service;

import com.lnu.entity.User;
import com.lnu.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!user.getPassword().equals(password)) throw new RuntimeException("密码错误");

        // 仅在非禁用状态下允许登录
        if ("disabled".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        user.setToken(UUID.randomUUID().toString());
        user.setPassword(null);
        return user;
    }

    @Override
    public List<User> getList(String roleType) {
        if ("student".equals(roleType)) {
            return userMapper.findStudents(); // 使用 Mapper 中定义的正确方法名
        } else {
            return userMapper.findManagers();
        }
    }

    @Override
    @Transactional
    public void saveUser(User user) {
        // 🟢 核心升级：尝试智能解析学号
        // 只有当角色是学生，且学号不为空时尝试解析
        if ("STUDENT".equals(user.getRole()) && user.getUsername() != null) {
            parseStudentIdSmart(user);
        }

        if (user.getId() == null) {
            User exist = userMapper.findByUsername(user.getUsername());
            if (exist != null) throw new RuntimeException("账号已存在");

            // 默认密码
            if (user.getPassword() == null) {
                user.setPassword("123456");
            }
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
    }

    @Override
    public void resetPassword(Long id) {
        userMapper.updatePassword(id, "123456");
    }

    /**
     * 🟢 智能学号解析引擎 (字典驱动版)
     * 规则示例: 2022 149 1 107 (11位)
     * [0-4] 年级: 2022
     * [4-7] 学院: 149 -> 查 sys_college 表
     * [7-8] 专业: 1   -> 查 sys_major 表
     * [8-9] 班级: 1
     * [9-11]座号: 07
     */
    private void parseStudentIdSmart(User user) {
        String sid = user.getUsername();

        // 1. 长度校验：只处理标准的 11 位学号
        // 如果是测试用的简单学号(如 2021001)，则跳过解析，避免覆盖手动填写的正确信息
        if (sid.length() != 11) {
            return;
        }

        try {
            // A. 解析年级 [0-4]
            String year = sid.substring(0, 4);
            user.setGrade(year + "级");

            // B. 解析学院 [4-7]
            String colCode = sid.substring(4, 7);
            user.setCollegeCode(colCode);
            // 查库获取学院名
            String collegeName = userMapper.findCollegeNameByCode(colCode);
            if (collegeName != null) {
                user.setCollege(collegeName);
            } else {
                user.setCollege("未知学院(" + colCode + ")");
            }

            // C. 解析专业 [7-8]
            String majCode = sid.substring(7, 8);
            user.setMajorCode(majCode);
            // 查库获取专业名
            String majorName = userMapper.findMajorNameByCode(colCode, majCode);
            if (majorName != null) {
                user.setMajor(majorName);
            } else {
                user.setMajor("未知专业(" + majCode + ")");
            }

            // D. 解析班级 [8-9]
            // 例如 '1' -> 1班, '2' -> 2班
            // 组合专业名生成班级名，例如：软件工程1班
            String classNumStr = sid.substring(8, 9);
            // 尝试转数字去掉前导0 (虽然这里只有1位)
            int classNum = Integer.parseInt(classNumStr);

            // 智能生成班级名：如果有专业名，就叫“软件工程1班”，否则叫“1班”
            if (user.getMajor() != null && !user.getMajor().startsWith("未知")) {
                user.setClassName(user.getMajor() + classNum + "班");
            } else {
                user.setClassName(classNum + "班");
            }

        } catch (Exception e) {
            System.err.println("学号解析异常: " + sid + "，将保留原始空值或手动输入值");
            // 吃掉异常，防止阻断保存流程
        }
    }
}