package com.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.java.ai.langchain4j.bean.UserAppointmentSummary;
import com.java.ai.langchain4j.entity.Appointment;
import com.java.ai.langchain4j.mapper.AppointmentMapper;
import com.java.ai.langchain4j.service.AppointmentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {
    /**
     * 查询订单是否存在
     *
     * @param appointment
     * @return
     */
    @Override
    public Appointment getOne(Appointment appointment) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        if (appointment.getUserId() != null) {
            queryWrapper.eq(Appointment::getUserId, appointment.getUserId());
        } else {
            queryWrapper.eq(Appointment::getUsername, appointment.getUsername());
            queryWrapper.eq(Appointment::getIdCard, appointment.getIdCard());
        }
        queryWrapper.eq(Appointment::getDepartment, appointment.getDepartment());
        queryWrapper.eq(Appointment::getDate, appointment.getDate());
        queryWrapper.eq(Appointment::getTime, appointment.getTime());
        Appointment appointmentDB = baseMapper.selectOne(queryWrapper);
        return appointmentDB;
    }

    /**
     * 统计指定医生在指定时段已预约数量。
     *
     * @param department 科室名称
     * @param date 预约日期
     * @param time 预约时间
     * @param doctorName 医生姓名
     * @return 已预约数量
     */
    @Override
    public long countBooked(String department, String date, String time, String doctorName) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getDepartment, department);
        queryWrapper.eq(Appointment::getDate, date);
        queryWrapper.eq(Appointment::getTime, time);
        queryWrapper.eq(Appointment::getDoctorName, doctorName);
        return baseMapper.selectCount(queryWrapper);
    }

    @Override
    public List<UserAppointmentSummary> listCurrentUserAppointments(Long userId, String username) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.eq(Appointment::getUserId, userId)
                .or()
                .eq(Appointment::getAccountUsername, username)
                .or()
                .eq(Appointment::getUsername, username));
        } else {
            queryWrapper.eq(Appointment::getUserId, userId);
        }
        queryWrapper.orderByDesc(Appointment::getDate)
            .orderByDesc(Appointment::getTime)
            .orderByDesc(Appointment::getId);
        List<Appointment> appointments = baseMapper.selectList(queryWrapper);
        return appointments.stream()
            .map(appointment -> new UserAppointmentSummary(
                appointment.getId(),
                appointment.getUsername(),
                maskIdCard(appointment.getIdCard()),
                appointment.getDepartment(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getDoctorName()
            ))
            .collect(Collectors.toList());
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }
}
