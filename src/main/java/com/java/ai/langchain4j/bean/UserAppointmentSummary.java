package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人中心预约记录摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAppointmentSummary {

    private Long id;

    private String patientName;

    private String idCard;

    private String department;

    private String date;

    private String time;

    private String doctorName;
}
