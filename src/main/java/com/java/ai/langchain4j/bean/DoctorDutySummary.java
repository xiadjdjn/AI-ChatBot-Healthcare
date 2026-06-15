package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生值班列表展示对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDutySummary {

    private Long id;

    private String doctorName;

    private String department;

    private String title;

    private String specialty;

    private Boolean enabled;

    private Boolean morningDuty;

    private Boolean afternoonDuty;

    private String currentSlot;

    private Boolean currentDuty;
}
