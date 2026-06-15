package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员查看用户预约信息摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAppointmentSummary {

    private Long id;

    private String accountUsername;

    private String patientName;

    private String idCard;

    private String department;

    private String date;

    private String time;

    private String doctorName;

    private String status;

    private LocalDateTime updatedAt;
}
