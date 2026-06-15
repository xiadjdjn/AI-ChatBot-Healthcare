package com.java.ai.langchain4j.service;

import com.java.ai.langchain4j.bean.AdminAppointmentSummary;
import com.java.ai.langchain4j.bean.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;
import com.java.ai.langchain4j.bean.UserAppointmentSummary;
import com.java.ai.langchain4j.entity.Appointment;

import java.util.List;

public interface AppointmentService extends IService<Appointment> {

    String STATUS_IN_PROGRESS = "进行中";
    String STATUS_CANCELLED = "已取消";
    String STATUS_COMPLETED = "已完成";

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

    /**
     * 管理员分页查看所有用户预约记录。
     *
     * @param keyword 用户名、就诊人、科室或医生关键字
     * @param status 预约状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 预约分页结果
     */
    PageResult<AdminAppointmentSummary> listAdminAppointments(String keyword, String status, Integer pageNum, Integer pageSize);
}
