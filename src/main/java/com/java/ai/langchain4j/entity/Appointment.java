package com.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String accountUsername;
    private String username;
    private String idCard;
    private String department;  //科室
    private String date;    //预约日期
    private String time;    //上午下午
    private String doctorName; //医生姓名
    private String status; //预约状态：进行中、已取消、已完成
    @TableField("updated_at")
    private LocalDateTime updatedAt; //更新时间
}
