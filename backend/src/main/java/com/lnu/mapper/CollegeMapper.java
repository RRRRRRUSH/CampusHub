package com.lnu.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollegeMapper {

    // 根据学院名称查校区
    // 对应你数据库 sys_college 表的 name 和 campus 字段
    @Select("SELECT campus FROM sys_college WHERE name = #{name}")
    String findCampusByCollege(String name);
}