package com.java.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.java.ai.langchain4j.bean.UserAppointmentSummary;
import com.java.ai.langchain4j.entity.Appointment;

import java.util.List;

public interface AppointmentService extends IService<Appointment> {
    Appointment getOne(Appointment appointment);

    /**
     * 统计指定医生在指定时段已预约数量。
     *
     * @param department 科室名称
     * @param date 预约日期
     * @param time 预约时间
     * @param doctorName 医生姓名
     * @return 已预约数量
     */
    long countBooked(String department, String date, String time, String doctorName);

    /**
     * 查询当前登录用户的全部预约记录。
     *
     * @param userId 登录用户 ID
     * @return 预约记录列表
     */
    List<UserAppointmentSummary> listCurrentUserAppointments(Long userId, String username);
}
