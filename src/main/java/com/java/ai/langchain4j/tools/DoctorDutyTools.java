package com.java.ai.langchain4j.tools;

import com.java.ai.langchain4j.bean.DoctorDutySummary;
import com.java.ai.langchain4j.service.DoctorScheduleService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DoctorDutyTools {

    private static final int MAX_DIRECT_DOCTOR_COUNT = 8;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    /**
     * 查询当前时段值班医生。
     *
     * @param department 科室名称，可为空；为空时返回当前时段全部值班医生
     * @param slot 时段，可为空；可选值包括今天上午、今天下午、当前、现在、上午、下午
     * @return 当前值班医生查询结果
     */
    @Tool(name = "查询当前值班医生", value = "根据科室名称和时段查询当前值班医生。若用户未提供科室，不要先追问，直接查询当前时段全部值班医生；若结果较多，先按科室汇总，再提示用户可以继续按科室筛选。")
    public String queryCurrentDoctorDuty(
        @P(value = "科室名称", required = false) String department,
        @P(value = "时段，可为空；可选值包括今天上午、今天下午、当前、现在、上午、下午", required = false) String slot
    ) {
        List<DoctorDutySummary> duties = doctorScheduleService.listCurrentDoctorDuties(slot);
        String normalizedDepartment = normalizeDepartment(department);
        if (StringUtils.hasText(normalizedDepartment)) {
            duties = duties.stream()
                .filter(item -> normalizedDepartment.equals(item.getDepartment()))
                .toList();
        }
        if (duties.isEmpty()) {
            return buildEmptyResult(normalizedDepartment, resolveSlot(slot));
        }
        String currentSlot = resolveCurrentSlot(duties, slot);
        if (duties.size() > MAX_DIRECT_DOCTOR_COUNT && !StringUtils.hasText(normalizedDepartment)) {
            return buildSummaryResult(duties, currentSlot);
        }
        return buildDetailResult(duties, normalizedDepartment, currentSlot);
    }

    private String normalizeDepartment(String department) {
        return StringUtils.hasText(department) ? department.trim() : "";
    }

    private String resolveCurrentSlot(List<DoctorDutySummary> duties, String slot) {
        if (duties != null && !duties.isEmpty() && StringUtils.hasText(duties.get(0).getCurrentSlot())) {
            return duties.get(0).getCurrentSlot();
        }
        return resolveSlot(slot);
    }

    private String resolveSlot(String slot) {
        if (!StringUtils.hasText(slot)) {
            return "当前时段";
        }
        String normalized = slot.trim();
        if (normalized.contains("上午")) {
            return "上午";
        }
        if (normalized.contains("下午")) {
            return "下午";
        }
        return "当前时段";
    }

    private String buildEmptyResult(String department, String slot) {
        if (StringUtils.hasText(department)) {
            return "查询结果：未查询到" + slot + department + "的值班医生。";
        }
        return "查询结果：未查询到" + slot + "的值班医生。";
    }

    private String buildSummaryResult(List<DoctorDutySummary> duties, String currentSlot) {
        Map<String, List<DoctorDutySummary>> grouped = groupByDepartment(duties);
        StringBuilder builder = new StringBuilder();
        builder.append("查询结果：").append(currentSlot).append("值班医生较多，先按科室汇总如下：");
        for (Map.Entry<String, List<DoctorDutySummary>> entry : grouped.entrySet()) {
            String doctorNames = entry.getValue().stream()
                .map(DoctorDutySummary::getDoctorName)
                .collect(Collectors.joining("、"));
            builder.append("\n- ").append(entry.getKey()).append("：").append(doctorNames);
        }
        builder.append("\n可继续按科室查询具体值班医生。");
        return builder.toString();
    }

    private String buildDetailResult(List<DoctorDutySummary> duties, String department, String currentSlot) {
        Map<String, List<DoctorDutySummary>> grouped = groupByDepartment(duties);
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(department)) {
            builder.append("查询结果：").append(currentSlot).append(department).append("值班医生如下：");
        } else {
            builder.append("查询结果：").append(currentSlot).append("值班医生如下：");
        }
        for (Map.Entry<String, List<DoctorDutySummary>> entry : grouped.entrySet()) {
            builder.append("\n- ").append(entry.getKey()).append("：");
            String doctors = entry.getValue().stream()
                .map(this::formatDoctor)
                .collect(Collectors.joining("；"));
            builder.append(doctors);
        }
        return builder.toString();
    }

    private Map<String, List<DoctorDutySummary>> groupByDepartment(List<DoctorDutySummary> duties) {
        Map<String, List<DoctorDutySummary>> grouped = new LinkedHashMap<>();
        for (DoctorDutySummary duty : duties) {
            grouped.computeIfAbsent(duty.getDepartment(), key -> new java.util.ArrayList<>()).add(duty);
        }
        return grouped;
    }

    private String formatDoctor(DoctorDutySummary summary) {
        StringBuilder builder = new StringBuilder(summary.getDoctorName());
        if (StringUtils.hasText(summary.getTitle())) {
            builder.append("（").append(summary.getTitle()).append("）");
        }
        if (StringUtils.hasText(summary.getSpecialty())) {
            builder.append("，擅长：").append(summary.getSpecialty());
        }
        return builder.toString();
    }
}
