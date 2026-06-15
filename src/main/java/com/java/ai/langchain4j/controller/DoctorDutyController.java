package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.bean.DoctorDutyForm;
import com.java.ai.langchain4j.bean.DoctorDutyStats;
import com.java.ai.langchain4j.bean.DoctorDutyStatusForm;
import com.java.ai.langchain4j.bean.DoctorDutySummary;
import com.java.ai.langchain4j.service.DoctorScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 值班医生管理接口。
 */
@Tag(name = "Doctor Duty")
@RestController
@RequestMapping("/doctor-duties")
public class DoctorDutyController {

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Operation(summary = "管理员查询医生值班列表")
    @GetMapping
    public ApiResponse<List<DoctorDutySummary>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                     @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                     @RequestParam(value = "slot", required = false) String slot) {
        return ApiResponse.success(doctorScheduleService.listDoctorDuties(keyword, enabled, slot));
    }

    @Operation(summary = "普通用户查询当前时段值班医生")
    @GetMapping("/current")
    public ApiResponse<List<DoctorDutySummary>> current(@RequestParam(value = "slot", required = false) String slot) {
        return ApiResponse.success(doctorScheduleService.listCurrentDoctorDuties(slot));
    }

    @Operation(summary = "管理员查询医生值班统计")
    @GetMapping("/stats")
    public ApiResponse<DoctorDutyStats> stats() {
        return ApiResponse.success(doctorScheduleService.getDutyStats());
    }

    @Operation(summary = "管理员新增医生")
    @PostMapping
    public ApiResponse<DoctorDutySummary> create(@RequestBody DoctorDutyForm form) {
        return ApiResponse.success(doctorScheduleService.createDoctorDuty(form));
    }

    @Operation(summary = "管理员修改医生信息")
    @PutMapping("/{id}")
    public ApiResponse<DoctorDutySummary> update(@PathVariable Long id,
                                                 @RequestBody DoctorDutyForm form) {
        return ApiResponse.success(doctorScheduleService.updateDoctorDuty(id, form));
    }

    @Operation(summary = "管理员调整医生值班状态")
    @PatchMapping("/{id}/duty")
    public ApiResponse<DoctorDutySummary> updateDuty(@PathVariable Long id,
                                                     @RequestBody DoctorDutyStatusForm form) {
        return ApiResponse.success(doctorScheduleService.updateDutyStatus(id, form));
    }

    @Operation(summary = "管理员删除医生")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success("delete success", doctorScheduleService.deleteDoctorDuty(id));
    }
}
