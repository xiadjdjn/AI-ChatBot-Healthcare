package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 医生值班维护请求。
 */
@Data
public class DoctorDutyForm {

    private String doctorName;

    private String department;

    private String title;

    private String specialty;

    private Integer enabled;

    private Integer morningDuty;

    private Integer afternoonDuty;

    private Integer morningTotalCount;

    private Integer afternoonTotalCount;
}
