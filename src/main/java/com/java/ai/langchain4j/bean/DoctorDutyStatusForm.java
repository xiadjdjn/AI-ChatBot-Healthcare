package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 医生值班状态调整请求。
 */
@Data
public class DoctorDutyStatusForm {

    private Integer enabled;

    private Integer morningDuty;

    private Integer afternoonDuty;
}
