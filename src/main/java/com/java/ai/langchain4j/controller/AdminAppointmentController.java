package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.bean.AdminAppointmentSummary;
import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员预约查看接口。
 */
@Tag(name = "Admin Appointment")
@RestController
@RequestMapping("/admin/appointments")
public class AdminAppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * 分页查看所有用户的预约记录。
     *
     * @param keyword 用户名、就诊人、科室或医生关键字
     * @param status 预约状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 预约分页结果
     */
    @Operation(summary = "管理员分页查看用户预约记录")
    @GetMapping
    public ApiResponse<PageResult<AdminAppointmentSummary>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success(appointmentService.listAdminAppointments(keyword, status, pageNum, pageSize));
    }
}
