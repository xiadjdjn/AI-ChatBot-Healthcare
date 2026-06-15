package com.java.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.java.ai.langchain4j.bean.DoctorDutyForm;
import com.java.ai.langchain4j.bean.DoctorDutyStats;
import com.java.ai.langchain4j.bean.DoctorDutyStatusForm;
import com.java.ai.langchain4j.bean.DoctorDutySummary;
import com.java.ai.langchain4j.entity.DoctorSchedule;

import java.util.List;

public interface DoctorScheduleService extends IService<DoctorSchedule> {

    /**
     * 查询指定条件下是否还有可预约号源。
     */
    boolean hasAvailableSchedule(String department, String date, String time, String doctorName);

    /**
     * 查询一条可预约排班。
     */
    DoctorSchedule findAvailableSchedule(String department, String date, String time, String doctorName);

    /**
     * 查询医生值班列表。
     */
    List<DoctorDutySummary> listDoctorDuties(String keyword, Boolean enabled, String slot);

    /**
     * 查询当前时段值班医生。
     */
    List<DoctorDutySummary> listCurrentDoctorDuties(String slot);

    /**
     * 查询值班统计。
     */
    DoctorDutyStats getDutyStats();

    /**
     * 新增医生。
     */
    DoctorDutySummary createDoctorDuty(DoctorDutyForm form);

    /**
     * 更新医生。
     */
    DoctorDutySummary updateDoctorDuty(Long id, DoctorDutyForm form);

    /**
     * 删除医生。
     */
    boolean deleteDoctorDuty(Long id);

    /**
     * 调整医生上午/下午值班状态。
     */
    DoctorDutySummary updateDutyStatus(Long id, DoctorDutyStatusForm form);
}
