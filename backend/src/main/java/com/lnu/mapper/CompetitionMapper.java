package com.lnu.mapper;

import com.lnu.entity.Competition;
import com.lnu.entity.CompetitionMember;
import com.lnu.entity.CompetitionTeam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CompetitionMapper {

    // --- 🟢 核心修改 1：管理端列表 (支持部门隔离) ---
    // 使用 <script> 标签实现动态 SQL
    // 如果 dept 不为空，则只查该部门发布的；如果为空 (null)，则查全部
    @Select("<script>" +
            "SELECT * FROM competition " +
            "WHERE source_type = #{sourceType} " +
            "<if test='dept != null and dept != \"\"'> AND publish_dept = #{dept} </if> " +
            "ORDER BY id DESC" +
            "</script>")
    List<Competition> findBySourceType(@Param("sourceType") String sourceType, @Param("dept") String dept);


    // --- 🟢 核心修改 2：学生端列表 (支持可见性过滤) ---
    // 逻辑：(不限学院 OR 学院匹配) AND (不限年级 OR 年级匹配) ...
    @Select("<script>" +
            "SELECT * FROM competition " +
            "WHERE source_type = #{sourceType} " +
            "AND status != 'finished' " + // 学生端通常不看已归档的（或者您想看也可以去掉这行）
            "AND (limit_college IS NULL OR limit_college = '' OR limit_college = #{college}) " +
            "AND (limit_grade IS NULL OR limit_grade = '' OR limit_grade = #{grade}) " +
            // 注：limit_campus 逻辑同理，如果 User 表里有 campus 字段可以加，暂时先注释防止报错
            // "AND (limit_campus IS NULL OR limit_campus = '' OR limit_campus = #{campus}) " +
            "ORDER BY id DESC" +
            "</script>")
    List<Competition> findForStudent(@Param("sourceType") String sourceType,
                                     @Param("college") String college,
                                     @Param("grade") String grade);


    @Select("SELECT * FROM competition WHERE id = #{id}")
    Competition findById(Long id);

    // --- 🟢 更新 Insert 和 Update，加入新字段 ---

    @Insert("INSERT INTO competition(title, level, source_type, status, mode, format, " +
            "reg_start_time, reg_end_time, comp_start_time, comp_end_time, " +
            "description, external_link, is_qualified, joined_count, " +
            "publish_dept, limit_college, limit_grade, limit_campus) " +
            "VALUES(#{title}, #{level}, #{sourceType}, #{status}, #{mode}, #{format}, " +
            "#{regStartTime}, #{regEndTime}, #{compStartTime}, #{compEndTime}, " +
            "#{description}, #{externalLink}, #{isQualified}, 0, " +
            "#{publishDept}, #{limitCollege}, #{limitGrade}, #{limitCampus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Competition competition);

    @Update("UPDATE competition SET title=#{title}, level=#{level}, mode=#{mode}, format=#{format}, " +
            "reg_start_time=#{regStartTime}, reg_end_time=#{regEndTime}, " +
            "comp_start_time=#{compStartTime}, comp_end_time=#{compEndTime}, " +
            "description=#{description}, external_link=#{externalLink}, is_qualified=#{isQualified}, " +
            "limit_college=#{limitCollege}, limit_grade=#{limitGrade}, limit_campus=#{limitCampus} " +
            "WHERE id = #{id}")
    void update(Competition competition);

    // --- 其他原有方法保持不变 ---

    @Update("UPDATE competition SET status = #{status} WHERE id = #{id}")
    void updateStatus(Long id, String status);

    @Update("UPDATE competition SET joined_count = IFNULL(joined_count, 0) + 1 WHERE id = #{id}")
    void incrementJoinedCount(Long id);

    @Delete("DELETE FROM competition WHERE id = #{id}")
    void delete(Long id);

    @Select("SELECT * FROM competition_team WHERE event_id = #{eventId} ORDER BY id DESC")
    List<CompetitionTeam> findTeamsByEventId(Long eventId);

    @Select("SELECT * FROM competition_member WHERE team_id = #{teamId}")
    List<CompetitionMember> findMembersByTeamId(Long teamId);

    @Select("SELECT * FROM competition WHERE status != 'finished' AND status != 'publicity'")
    List<Competition> findActiveCompetitions();
}