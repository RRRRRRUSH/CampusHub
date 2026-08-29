package com.lnu.mapper;

import com.lnu.entity.AuditRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface AuditMapper {

    // 1. 管理端：查询全部
    @Select("SELECT r.*, " +
            "u.name as studentName, u.username as studentId, u.college, u.major, u.class_name as className " +
            "FROM sys_audit_record r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.type = #{type} " +
            "ORDER BY r.create_time DESC")
    List<AuditRecord> findByType(@Param("type") String type);

    // 2. 管理端：按班级查询
    @Select("SELECT r.*, " +
            "u.name as studentName, u.username as studentId, u.college, u.major, u.class_name as className " +
            "FROM sys_audit_record r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.type = #{type} AND u.class_name = #{className} " +
            "ORDER BY r.create_time DESC")
    List<AuditRecord> findByTypeAndClass(@Param("type") String type, @Param("className") String className);

    // 3. 学生端查询
    @Select("SELECT *, " +
            "CASE WHEN title LIKE '%校内%' THEN 'internal' ELSE 'external' END as sourceType " +
            "FROM sys_audit_record " +
            "WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC")
    List<Map<String, Object>> findByUserId(Long userId);

    // 4. 提交申请
    @Insert("INSERT INTO sys_audit_record(user_id, type, source_type, origin_id, title, score, award_level, " +
            "proof_url, description, status, apply_time) " +
            "VALUES(#{userId}, #{type}, #{sourceType}, #{originId}, #{title}, #{score}, #{awardLevel}, " +
            "#{proofUrl}, #{description}, #{status}, #{applyTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AuditRecord record);

    // 5. 审核操作
    @Update("UPDATE sys_audit_record SET status = 'approved' WHERE id = #{id}")
    void approve(Long id);

    @Update("UPDATE sys_audit_record SET status = 'rejected', reject_reason = #{reason} WHERE id = #{id}")
    void reject(@Param("id") Long id, @Param("reason") String reason);

    // 6. 重置状态 (认定回退)
    @Update("UPDATE sys_audit_record SET status = 'pending', reject_reason = NULL WHERE id = #{id}")
    void reset(Long id);

    // 🟢 7. [新增] 按来源ID删除 (活动回退专用)
    // 用于在活动从"已归档"回退时，清理掉自动生成的记录
    @Delete("DELETE FROM sys_audit_record WHERE origin_id = #{originId}")
    void deleteByOriginId(Long originId);

    @Select("SELECT * FROM sys_audit_record WHERE id = #{id}")
    AuditRecord findById(Long id);

    // 加分操作
    @Update("UPDATE sys_user SET score_volunteer = score_volunteer + #{delta} WHERE id = #{userId}")
    void addVolunteerScore(@Param("userId") Long userId, @Param("delta") Double delta);

    @Update("UPDATE sys_user SET score_competition = score_competition + #{delta} WHERE id = #{userId}")
    void addCompetitionScore(@Param("userId") Long userId, @Param("delta") Double delta);
}