package com.lnu.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class Activity {
    private Long id;
    private String title;
    private String sourceType; // internal, external
    private String format;     // online, offline
    private String location;
    private Boolean needPhoto;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date regStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date regEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date activityStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date activityEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishTime;

    private Double hours;
    private Integer quota;
    private Integer joined;
    private String status;
    private String description;

    // 数据库存的是 String (逗号分隔)，但为了接收前端数组，我们加 @Transient 或直接用 Object
    // 这里为了方便 MyBatis 映射，我们定义为 String
    // 前端传来的数组需要在 Controller/Service 层转成 String
    private String limitCampus;
    private String limitCollege;
    private String limitGrade;

    // 辅助字段：接收前端传来的数组 (不存库)
    // 注意：需要在 Controller 手动把 List 转成上面的 String
    // 下面这部分需要加 @Transient (如果用 JPA) 或者在 Mapper XML 里忽略
    // 简单起见，我们在 Controller 里接收 Map 或 DTO，这里只写跟数据库对应的字段
}