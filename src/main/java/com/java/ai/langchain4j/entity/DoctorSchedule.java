package com.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生值班信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("doctor_schedule")
public class DoctorSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String department;

    private String doctorName;

    private String title;

    private String specialty;

    private String time;

    private Integer totalCount;

    private Integer doctorEnabled;

    private Integer enabled;
}
