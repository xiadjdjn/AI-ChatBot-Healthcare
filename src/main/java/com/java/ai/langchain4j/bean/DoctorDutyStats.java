package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生值班统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDutyStats {

    private Long totalDoctors;

    private Long enabledDoctors;

    private Long currentDutyDoctors;
}
