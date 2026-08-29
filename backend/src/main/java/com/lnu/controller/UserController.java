package com.lnu.controller;

import com.lnu.common.Result;
import com.lnu.entity.User;
import com.lnu.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;

    // 1. 登录
    @PostMapping("/login")
    public Result login(@RequestBody User loginUser) {
        User user = userMapper.findByUsername(loginUser.getUsername());
        if (user == null) return Result.error("用户不存在");
        if (!user.getPassword().equals(loginUser.getPassword())) return Result.error("密码错误");
        if ("disabled".equals(user.getStatus())) return Result.error("账号已被禁用");

        String token = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("token", token);
        data.put("college", user.getCollege());
        data.put("isCadre", user.getIsCadre());
        data.put("className", user.getClassName());

        return Result.success(data);
    }

    // 🟢 2. 管理员查询列表 (升级逻辑：学号前9位匹配)
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String role,
                       @RequestParam Long currentUserId) {

        User operator = userMapper.findById(currentUserId);
        if (operator == null) return Result.error("操作人不存在");

        String filterCollege = null;
        String filterClassPrefix = null; // 🟢 以前是 filterClass (String)

        // --- 权限判定 ---

        // A. 学生干部 -> 截取学号前9位作为班级标识
        if ("STUDENT".equals(operator.getRole())) {
            if (Boolean.TRUE.equals(operator.getIsCadre())) {
                String sid = operator.getUsername();
                if (sid != null && sid.length() >= 9) {
                    // 🟢 核心逻辑：取前9位 (年级4+学院3+专业1+班级1)
                    filterClassPrefix = sid.substring(0, 9);
                } else {
                    return Result.error("您的学号格式不标准，无法定位班级权限");
                }
            } else {
                return Result.error("无权访问");
            }
        }

        // B. 普通教师 -> 只能查本院
        else if ("TEACHER".equals(operator.getRole())) {
            filterCollege = operator.getCollege();
            if (filterCollege == null) return Result.error("未绑定学院信息");
        }

        // C. 管理员 -> 查全校

        // 执行查询
        return Result.success(userMapper.search(keyword, role, filterCollege, filterClassPrefix));
    }

    // --- 其他接口保持不变 ---
    @PostMapping("/reset-password")
    public Result resetPassword(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        userMapper.updatePassword(id, "123456");
        return Result.success();
    }

    @PostMapping("/status")
    public Result toggleStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String status = (String) params.get("status");
        userMapper.toggleStatus(id, status);
        return Result.success();
    }

    @PostMapping("/set-cadre")
    public Result setCadre(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Boolean isCadre = (Boolean) params.get("isCadre");
        userMapper.setCadre(id, isCadre);
        return Result.success();
    }

    @GetMapping("/info/{id}")
    public Result getInfo(@PathVariable Long id) {
        User user = userMapper.findById(id);
        if(user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/update/info")
    public Result updateInfo(@RequestBody User user) {
        userMapper.updateInfo(user);
        return Result.success();
    }

    @PostMapping("/bind/email")
    public Result bindEmail(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String email = (String) params.get("email");
        userMapper.updateEmail(userId, email);
        return Result.success();
    }

    @PostMapping("/update/password")
    public Result updatePwd(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String oldPwd = (String) params.get("oldPassword");
        String newPwd = (String) params.get("newPassword");
        User user = userMapper.findById(userId);
        if (!user.getPassword().equals(oldPwd)) return Result.error("旧密码错误");
        userMapper.updatePassword(userId, newPwd);
        return Result.success();
    }
}