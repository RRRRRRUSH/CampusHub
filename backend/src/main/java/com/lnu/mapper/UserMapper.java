package com.lnu.mapper;

import com.lnu.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    User findById(Long id);

    // --- 字典查询 ---
    @Select("SELECT name FROM sys_college WHERE code = #{code}")
    String findCollegeNameByCode(String code);

    @Select("SELECT name FROM sys_major WHERE college_code = #{collegeCode} AND code = #{majorCode}")
    String findMajorNameByCode(@Param("collegeCode") String collegeCode, @Param("majorCode") String majorCode);

    // --- 搜索 ---
    @Select("<script>" +
            "SELECT * FROM sys_user WHERE 1=1 " +
            "<if test='role != null and role != \"\"'> AND role = #{role} </if>" +
            "<if test='college != null and college != \"\"'> AND college = #{college} </if>" +
            "<if test='classPrefix != null and classPrefix != \"\"'> AND username LIKE CONCAT(#{classPrefix}, '%') </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (username LIKE CONCAT('%',#{keyword},'%') OR name LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "ORDER BY id DESC" +
            "</script>")
    List<User> search(@Param("keyword") String keyword,
                      @Param("role") String role,
                      @Param("college") String college,
                      @Param("classPrefix") String classPrefix);

    // --- 基础增改查 ---
    @Select("SELECT * FROM sys_user WHERE role != 'STUDENT' ORDER BY id DESC")
    List<User> findManagers();

    @Select("SELECT * FROM sys_user WHERE role = 'STUDENT' ORDER BY id DESC")
    List<User> findStudents();

    @Insert("INSERT INTO sys_user(username, password, name, role, status, college, major, grade, class_name, college_code, major_code, is_cadre, score_competition, score_volunteer, email) " +
            "VALUES(#{username}, #{password}, #{name}, #{role}, 'active', #{college}, #{major}, #{grade}, #{className}, #{collegeCode}, #{majorCode}, #{isCadre}, 0.0, 0.0, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Update("UPDATE sys_user SET name=#{name}, college=#{college}, major=#{major}, class_name=#{className}, role=#{role}, is_cadre=#{isCadre}, email=#{email}, grade=#{grade} WHERE id=#{id}")
    void update(User user);

    @Update("UPDATE sys_user SET college=#{college}, major=#{major}, class_name=#{className}, grade=#{grade} WHERE id=#{id}")
    void updateInfo(User user);

    @Update("UPDATE sys_user SET email = #{email} WHERE id = #{id}")
    void updateEmail(@Param("id") Long id, @Param("email") String email);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    void updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id}")
    void toggleStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE sys_user SET is_cadre = #{isCadre} WHERE id = #{id}")
    void setCadre(@Param("id") Long id, @Param("isCadre") Boolean isCadre);

    // --- 加分方法 ---
    @Update("UPDATE sys_user SET score_competition = score_competition + #{delta} WHERE id = #{id}")
    void addCompetitionScore(@Param("id") Long id, @Param("delta") Double delta);

    @Update("UPDATE sys_user SET score_volunteer = score_volunteer + #{delta} WHERE id = #{id}")
    void addVolunteerScore(@Param("id") Long id, @Param("delta") Double delta);

    // --- 🟢 新增：扣分方法 (用于回退操作) ---
    @Update("UPDATE sys_user SET score_competition = score_competition - #{delta} WHERE id = #{id}")
    void deductCompetitionScore(@Param("id") Long id, @Param("delta") Double delta);

    @Update("UPDATE sys_user SET score_volunteer = score_volunteer - #{delta} WHERE id = #{id}")
    void deductVolunteerScore(@Param("id") Long id, @Param("delta") Double delta);
}