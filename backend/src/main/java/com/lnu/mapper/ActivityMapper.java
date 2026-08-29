package com.lnu.mapper;

import com.lnu.entity.Activity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    // 🟢 核心修改：升级列表查询，支持校区过滤
    // 逻辑：如果 userCampus 为空(管理员)，则查所有；
    // 如果 userCampus 有值(学生)，则查无限制的 + 包含自己校区的。
    @Select("SELECT * FROM sys_activity " +
            "WHERE source_type = #{type} " +
            "AND (" +
            "   #{userCampus} IS NULL " +
            "   OR limit_campus IS NULL " +
            "   OR limit_campus = '' " +
            "   OR limit_campus LIKE CONCAT('%', #{userCampus}, '%')" +
            ") " +
            "ORDER BY id DESC")
    List<Activity> findByType(@Param("type") String type, @Param("userCampus") String userCampus);

    @Select("SELECT * FROM sys_activity WHERE id = #{id}")
    Activity findById(Long id);

    @Insert("INSERT INTO sys_activity(title, source_type, format, location, need_photo, " +
            "reg_start_time, reg_end_time, activity_start_time, activity_end_time, " +
            "hours, quota, joined, status, limit_campus, limit_college, limit_grade, description, publish_time) " +
            "VALUES(#{title}, #{sourceType}, #{format}, #{location}, #{needPhoto}, " +
            "#{regStartTime}, #{regEndTime}, #{activityStartTime}, #{activityEndTime}, " +
            "#{hours}, #{quota}, 0, #{status}, #{limitCampus}, #{limitCollege}, #{limitGrade}, #{description}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Activity activity);

    @Update("UPDATE sys_activity SET title=#{title}, format=#{format}, location=#{location}, need_photo=#{needPhoto}, " +
            "reg_start_time=#{regStartTime}, reg_end_time=#{regEndTime}, activity_start_time=#{activityStartTime}, activity_end_time=#{activityEndTime}, " +
            "hours=#{hours}, quota=#{quota}, limit_campus=#{limitCampus}, limit_college=#{limitCollege}, limit_grade=#{limitGrade}, " +
            "description=#{description} WHERE id=#{id}")
    void update(Activity activity);

    @Delete("DELETE FROM sys_activity WHERE id = #{id}")
    void delete(Long id);

    // --- 报名相关 ---

    @Update("UPDATE sys_activity SET joined = joined + 1 WHERE id = #{id}")
    void increaseJoined(Long id);

    @Update("UPDATE sys_activity SET joined = joined - 1 WHERE id = #{id} AND joined > 0")
    void decreaseJoined(Long id);

    @Insert("INSERT INTO sys_activity_signup(activity_id, user_id, student_name, student_id, college, signup_time, status) " +
            "VALUES(#{activityId}, #{userId}, #{studentName}, #{studentId}, #{college}, NOW(), 'signed_up')")
    void insertSignup(@Param("activityId") Long activityId, @Param("userId") Long userId,
                      @Param("studentName") String studentName, @Param("studentId") String studentId, @Param("college") String college);

    @Select("SELECT count(*) FROM sys_activity_signup WHERE activity_id = #{activityId} AND user_id = #{userId}")
    int checkSignup(@Param("activityId") Long activityId, @Param("userId") Long userId);

    @Delete("DELETE FROM sys_activity_signup WHERE activity_id = #{activityId} AND user_id = #{userId}")
    int deleteSignup(@Param("activityId") Long activityId, @Param("userId") Long userId);

    // --- 查询相关 ---

    // 1. 获取参与名单 (前端展示用)
    @Select("SELECT id, user_id, student_name as name, student_id as sid, college, status, signup_time as signupTime, " +
            "CASE WHEN status = 'present' THEN 1 WHEN status = 'absent' THEN 0 ELSE null END as isValid " +
            "FROM sys_activity_signup WHERE activity_id = #{activityId} ORDER BY signup_time DESC")
    List<Map<String, Object>> getParticipants(Long activityId);

    // 2. 导出专用查询
    @Select("SELECT student_name, student_id, college, signup_time, status " +
            "FROM sys_activity_signup " +
            "WHERE activity_id = #{activityId} " +
            "ORDER BY student_id ASC")
    List<Map<String, Object>> selectParticipantsForExport(Long activityId);

    // 3. 结算专用查询
    @Select("SELECT user_id, student_name, student_id, college FROM sys_activity_signup " +
            "WHERE activity_id = #{activityId} AND status != 'absent'")
    List<Map<String, Object>> selectValidSignups(Long activityId);

    // 4. 我的活动列表
    @Select("SELECT a.*, s.status as signupStatus, s.signup_time as signupTime " +
            "FROM sys_activity a " +
            "JOIN sys_activity_signup s ON a.id = s.activity_id " +
            "WHERE s.user_id = #{userId} " +
            "ORDER BY s.signup_time DESC")
    List<Map<String, Object>> findMySignups(Long userId);

    // --- 更新相关 ---

    // 5. 变更活动状态
    @Update("UPDATE sys_activity SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // 6. 单个考勤核验
    @Update("UPDATE sys_activity_signup SET status = #{status} WHERE id = #{signupId}")
    void updateAttendanceStatus(@Param("signupId") Long signupId, @Param("status") String status);

    // 7. 批量更新报名状态 (使用 MyBatis 动态 SQL)
    @Update("<script>" +
            "UPDATE sys_activity_signup SET status = #{status} WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchUpdateSignupStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}