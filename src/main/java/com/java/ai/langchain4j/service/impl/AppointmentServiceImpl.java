package com.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.java.ai.langchain4j.bean.AdminAppointmentSummary;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.bean.UserAppointmentSummary;
import com.java.ai.langchain4j.entity.Appointment;
import com.java.ai.langchain4j.mapper.AppointmentMapper;
import com.java.ai.langchain4j.service.AppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    private static final List<Integer> ADMIN_PAGE_SIZE_OPTIONS = List.of(10, 15, 20);

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
        queryWrapper.eq(Appointment::getStatus, STATUS_IN_PROGRESS);
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
        queryWrapper.eq(Appointment::getStatus, STATUS_IN_PROGRESS);
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
                appointment.getDoctorName(),
                appointment.getStatus()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public PageResult<AdminAppointmentSummary> listAdminAppointments(String keyword, String status, Integer pageNum, Integer pageSize) {
        int resolvedPageNum = resolvePageNum(pageNum);
        int resolvedPageSize = resolveAdminPageSize(pageSize);

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper.like(Appointment::getAccountUsername, normalizedKeyword)
                .or()
                .like(Appointment::getUsername, normalizedKeyword)
                .or()
                .like(Appointment::getDepartment, normalizedKeyword)
                .or()
                .like(Appointment::getDoctorName, normalizedKeyword));
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Appointment::getStatus, status.trim());
        }
        long total = baseMapper.selectCount(queryWrapper);
        if (total == 0L) {
            return PageResult.<AdminAppointmentSummary>builder()
                .total(0L)
                .records(Collections.emptyList())
                .build();
        }

        long offset = (long) (resolvedPageNum - 1) * resolvedPageSize;
        queryWrapper.orderByDesc(Appointment::getUpdatedAt)
            .orderByDesc(Appointment::getId)
            .last("limit " + offset + "," + resolvedPageSize);
        List<Appointment> records = baseMapper.selectList(queryWrapper);
        return PageResult.<AdminAppointmentSummary>builder()
            .total(total)
            .records(records == null ? Collections.emptyList() : records.stream()
                .map(this::toAdminAppointmentSummary)
                .collect(Collectors.toList()))
            .build();
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    private AdminAppointmentSummary toAdminAppointmentSummary(Appointment appointment) {
        return new AdminAppointmentSummary(
            appointment.getId(),
            appointment.getAccountUsername(),
            appointment.getUsername(),
            maskIdCard(appointment.getIdCard()),
            appointment.getDepartment(),
            appointment.getDate(),
            appointment.getTime(),
            appointment.getDoctorName(),
            appointment.getStatus(),
            appointment.getUpdatedAt()
        );
    }

    private int resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    private int resolveAdminPageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (!ADMIN_PAGE_SIZE_OPTIONS.contains(pageSize)) {
            throw new IllegalArgumentException("pageSize must be 10, 15 or 20");
        }
        return pageSize;
    }
}
