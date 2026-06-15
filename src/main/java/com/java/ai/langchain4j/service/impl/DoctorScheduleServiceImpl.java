package com.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.java.ai.langchain4j.bean.DoctorDutyForm;
import com.java.ai.langchain4j.bean.DoctorDutyStats;
import com.java.ai.langchain4j.bean.DoctorDutyStatusForm;
import com.java.ai.langchain4j.bean.DoctorDutySummary;
import com.java.ai.langchain4j.entity.DoctorSchedule;
import com.java.ai.langchain4j.mapper.DoctorScheduleMapper;
import com.java.ai.langchain4j.service.AppointmentService;
import com.java.ai.langchain4j.service.DoctorScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorScheduleServiceImpl extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule> implements DoctorScheduleService {

    private static final String MORNING = "上午";
    private static final String AFTERNOON = "下午";
    private static final int DEFAULT_TOTAL_COUNT = 10;

    @Autowired
    private AppointmentService appointmentService;

    @Override
    public boolean hasAvailableSchedule(String department, String date, String time, String doctorName) {
        return findAvailableSchedule(department, date, time, doctorName) != null;
    }

    @Override
    public DoctorSchedule findAvailableSchedule(String department, String date, String time, String doctorName) {
        String normalizedTime = normalizeSlot(time);
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department);
        queryWrapper.eq(DoctorSchedule::getTime, normalizedTime);
        queryWrapper.eq(DoctorSchedule::getEnabled, 1);
        if (StringUtils.hasText(doctorName)) {
            queryWrapper.eq(DoctorSchedule::getDoctorName, doctorName);
        }

        List<DoctorSchedule> schedules = baseMapper.selectList(queryWrapper);
        for (DoctorSchedule schedule : schedules) {
            if (!isDoctorEnabled(schedule)) {
                continue;
            }
            int totalCount = schedule.getTotalCount() == null ? 0 : schedule.getTotalCount();
            long bookedCount = appointmentService.countBooked(
                schedule.getDepartment(),
                date,
                normalizedTime,
                schedule.getDoctorName()
            );
            if (totalCount > bookedCount) {
                return schedule;
            }
        }
        return null;
    }

    @Override
    public List<DoctorDutySummary> listDoctorDuties(String keyword, Boolean enabled, String slot) {
        List<DoctorDutySummary> summaries = groupSchedules(loadSchedules(), null);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSlot = normalizeSlotFilter(slot);
        return summaries.stream()
            .filter(summary -> matchesKeyword(summary, normalizedKeyword))
            .filter(summary -> enabled == null || summary.getEnabled().equals(enabled))
            .filter(summary -> filterBySlot(summary, normalizedSlot))
            .sorted(Comparator.comparing(DoctorDutySummary::getDepartment, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DoctorDutySummary::getDoctorName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public List<DoctorDutySummary> listCurrentDoctorDuties(String slot) {
        String resolvedSlot = resolveCurrentSlot(slot);
        List<DoctorDutySummary> summaries = groupSchedules(loadSchedules(), resolvedSlot);
        for (DoctorDutySummary summary : summaries) {
            summary.setCurrentSlot(resolvedSlot);
            summary.setCurrentDuty(Boolean.TRUE);
        }
        return summaries;
    }

    @Override
    public DoctorDutyStats getDutyStats() {
        List<DoctorDutySummary> summaries = groupSchedules(loadSchedules(), null);
        String currentSlot = resolveCurrentSlot(null);
        long currentDutyCount = summaries.stream()
            .filter(summary -> filterBySlot(summary, currentSlot))
            .count();
        long enabledCount = summaries.stream()
            .filter(summary -> Boolean.TRUE.equals(summary.getEnabled()))
            .count();
        return new DoctorDutyStats((long) summaries.size(), enabledCount, currentDutyCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorDutySummary createDoctorDuty(DoctorDutyForm form) {
        validateForm(form);
        String department = form.getDepartment().trim();
        String doctorName = form.getDoctorName().trim();
        ensureDoctorNotExists(department, doctorName);

        boolean doctorEnabled = resolveBoolean(form.getEnabled(), true);
        saveBatch(buildSchedules(department, doctorName, form, doctorEnabled));
        return findDoctorSummary(department, doctorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorDutySummary updateDoctorDuty(Long id, DoctorDutyForm form) {
        validateForm(form);
        DoctorSchedule current = requireSchedule(id);
        List<DoctorSchedule> currentRows = listSchedulesByDoctor(current.getDepartment(), current.getDoctorName());
        String department = form.getDepartment().trim();
        String doctorName = form.getDoctorName().trim();
        ensureDoctorNameAvailable(currentRows, department, doctorName);

        boolean doctorEnabled = resolveBoolean(form.getEnabled(), isDoctorEnabled(current));
        for (DoctorSchedule schedule : currentRows) {
            DoctorSchedule update = new DoctorSchedule();
            update.setId(schedule.getId());
            update.setDepartment(department);
            update.setDoctorName(doctorName);
            update.setTitle(form.getTitle());
            update.setSpecialty(form.getSpecialty());
            update.setDoctorEnabled(doctorEnabled ? 1 : 0);
            update.setEnabled(isMorning(schedule.getTime())
                ? (resolveBoolean(form.getMorningDuty(), isSlotRowEnabled(schedule)) ? 1 : 0)
                : (resolveBoolean(form.getAfternoonDuty(), isSlotRowEnabled(schedule)) ? 1 : 0));
            update.setTotalCount(resolveTotalCount(schedule.getTime(), form, schedule.getTotalCount()));
            baseMapper.updateById(update);
        }
        return findDoctorSummary(department, doctorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDoctorDuty(Long id) {
        DoctorSchedule current = requireSchedule(id);
        List<Long> ids = listSchedulesByDoctor(current.getDepartment(), current.getDoctorName()).stream()
            .map(DoctorSchedule::getId)
            .toList();
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorDutySummary updateDutyStatus(Long id, DoctorDutyStatusForm form) {
        DoctorSchedule current = requireSchedule(id);
        boolean doctorEnabled = resolveBoolean(form == null ? null : form.getEnabled(), isDoctorEnabled(current));
        boolean morningDuty = resolveBoolean(form == null ? null : form.getMorningDuty(), isMorningEnabled(current.getDepartment(), current.getDoctorName()));
        boolean afternoonDuty = resolveBoolean(form == null ? null : form.getAfternoonDuty(), isAfternoonEnabled(current.getDepartment(), current.getDoctorName()));

        for (DoctorSchedule schedule : listSchedulesByDoctor(current.getDepartment(), current.getDoctorName())) {
            DoctorSchedule update = new DoctorSchedule();
            update.setId(schedule.getId());
            update.setDoctorEnabled(doctorEnabled ? 1 : 0);
            update.setEnabled(isMorning(schedule.getTime()) ? morningDuty ? 1 : 0 : afternoonDuty ? 1 : 0);
            baseMapper.updateById(update);
        }
        return findDoctorSummary(current.getDepartment(), current.getDoctorName());
    }

    private List<DoctorSchedule> loadSchedules() {
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(DoctorSchedule::getDepartment)
            .orderByAsc(DoctorSchedule::getDoctorName)
            .orderByAsc(DoctorSchedule::getTime)
            .orderByAsc(DoctorSchedule::getId);
        return baseMapper.selectList(queryWrapper);
    }

    private List<DoctorSchedule> listSchedulesByDoctor(String department, String doctorName) {
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department)
            .eq(DoctorSchedule::getDoctorName, doctorName)
            .orderByAsc(DoctorSchedule::getTime)
            .orderByAsc(DoctorSchedule::getId);
        return baseMapper.selectList(queryWrapper);
    }

    private List<DoctorDutySummary> groupSchedules(List<DoctorSchedule> schedules, String slotFilter) {
        Map<String, List<DoctorSchedule>> grouped = new LinkedHashMap<>();
        for (DoctorSchedule schedule : schedules) {
            String key = schedule.getDepartment() + "|" + schedule.getDoctorName();
            grouped.computeIfAbsent(key, ignore -> new ArrayList<>()).add(schedule);
        }

        List<DoctorDutySummary> summaries = new ArrayList<>();
        for (List<DoctorSchedule> doctorSchedules : grouped.values()) {
            DoctorDutySummary summary = toSummary(doctorSchedules);
            if (!StringUtils.hasText(slotFilter) || filterBySlot(summary, slotFilter)) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    private DoctorDutySummary toSummary(List<DoctorSchedule> doctorSchedules) {
        DoctorSchedule representative = doctorSchedules.stream()
            .min(Comparator.comparing(DoctorSchedule::getId))
            .orElseThrow();
        DoctorDutySummary summary = new DoctorDutySummary();
        summary.setId(representative.getId());
        summary.setDoctorName(representative.getDoctorName());
        summary.setDepartment(representative.getDepartment());
        summary.setTitle(representative.getTitle());
        summary.setSpecialty(representative.getSpecialty());
        summary.setEnabled(doctorSchedules.stream().allMatch(this::isDoctorEnabled));
        summary.setMorningDuty(isSlotEnabled(doctorSchedules, MORNING));
        summary.setAfternoonDuty(isSlotEnabled(doctorSchedules, AFTERNOON));
        return summary;
    }

    private boolean isSlotEnabled(List<DoctorSchedule> doctorSchedules, String slot) {
        return doctorSchedules.stream()
            .filter(item -> slot.equals(normalizeSlot(item.getTime())))
            .anyMatch(item -> isDoctorEnabled(item) && isSlotRowEnabled(item));
    }

    private boolean isSlotRowEnabled(DoctorSchedule schedule) {
        return schedule.getEnabled() != null && schedule.getEnabled() == 1;
    }

    private boolean isDoctorEnabled(DoctorSchedule schedule) {
        return schedule.getDoctorEnabled() == null || schedule.getDoctorEnabled() == 1;
    }

    private boolean isMorningEnabled(String department, String doctorName) {
        return listSchedulesByDoctor(department, doctorName).stream()
            .filter(item -> MORNING.equals(normalizeSlot(item.getTime())))
            .findFirst()
            .map(this::isSlotRowEnabled)
            .orElse(false);
    }

    private boolean isAfternoonEnabled(String department, String doctorName) {
        return listSchedulesByDoctor(department, doctorName).stream()
            .filter(item -> AFTERNOON.equals(normalizeSlot(item.getTime())))
            .findFirst()
            .map(this::isSlotRowEnabled)
            .orElse(false);
    }

    private boolean filterBySlot(DoctorDutySummary summary, String slot) {
        if (!StringUtils.hasText(slot)) {
            return true;
        }
        return switch (normalizeSlotFilter(slot)) {
            case MORNING -> Boolean.TRUE.equals(summary.getMorningDuty());
            case AFTERNOON -> Boolean.TRUE.equals(summary.getAfternoonDuty());
            default -> true;
        };
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
    }

    private boolean matchesKeyword(DoctorDutySummary summary, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String text = String.join(" ",
            safeLower(summary.getDoctorName()),
            safeLower(summary.getDepartment()),
            safeLower(summary.getTitle()),
            safeLower(summary.getSpecialty()));
        return text.contains(keyword);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String normalizeSlotFilter(String slot) {
        if (!StringUtils.hasText(slot)) {
            return "";
        }
        String normalized = slot.trim().toUpperCase();
        if (normalized.contains("AM") || normalized.contains("MORNING") || normalized.contains(MORNING)) {
            return MORNING;
        }
        if (normalized.contains("PM") || normalized.contains("AFTERNOON") || normalized.contains(AFTERNOON)) {
            return AFTERNOON;
        }
        return normalizeSlot(slot);
    }

    private String resolveCurrentSlot(String slot) {
        if (StringUtils.hasText(slot)) {
            return normalizeSlotFilter(slot);
        }
        return LocalTime.now().getHour() < 12 ? MORNING : AFTERNOON;
    }

    private String normalizeSlot(String time) {
        if (!StringUtils.hasText(time)) {
            return "";
        }
        String normalized = time.trim().toUpperCase();
        if (normalized.contains("AM") || normalized.contains("MORNING") || normalized.contains(MORNING) || normalized.contains("早上")) {
            return MORNING;
        }
        if (normalized.contains("PM") || normalized.contains("AFTERNOON") || normalized.contains(AFTERNOON)
            || normalized.contains("中午") || normalized.contains("晚上")) {
            return AFTERNOON;
        }
        return time.trim();
    }

    private boolean isMorning(String time) {
        return MORNING.equals(normalizeSlot(time));
    }

    private List<DoctorSchedule> buildSchedules(String department, String doctorName, DoctorDutyForm form, boolean doctorEnabled) {
        List<DoctorSchedule> schedules = new ArrayList<>();
        schedules.add(buildSchedule(department, doctorName, form, doctorEnabled, MORNING, resolveBoolean(form.getMorningDuty(), true), resolveTotalCount(MORNING, form, null)));
        schedules.add(buildSchedule(department, doctorName, form, doctorEnabled, AFTERNOON, resolveBoolean(form.getAfternoonDuty(), true), resolveTotalCount(AFTERNOON, form, null)));
        return schedules;
    }

    private DoctorSchedule buildSchedule(String department, String doctorName, DoctorDutyForm form, boolean doctorEnabled, String time, boolean slotEnabled, int totalCount) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDepartment(department);
        schedule.setDoctorName(doctorName);
        schedule.setTitle(form.getTitle());
        schedule.setSpecialty(form.getSpecialty());
        schedule.setTime(time);
        schedule.setTotalCount(totalCount);
        schedule.setDoctorEnabled(doctorEnabled ? 1 : 0);
        schedule.setEnabled(slotEnabled ? 1 : 0);
        return schedule;
    }

    private int resolveTotalCount(String time, DoctorDutyForm form, Integer fallback) {
        Integer totalCount = MORNING.equals(normalizeSlot(time)) ? form.getMorningTotalCount() : form.getAfternoonTotalCount();
        if (totalCount == null || totalCount <= 0) {
            return fallback == null || fallback <= 0 ? DEFAULT_TOTAL_COUNT : fallback;
        }
        return totalCount;
    }

    private boolean resolveBoolean(Integer value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value == 1;
    }

    private void validateForm(DoctorDutyForm form) {
        if (form == null) {
            throw new IllegalArgumentException("doctor duty form cannot be null");
        }
        if (!StringUtils.hasText(form.getDoctorName()) || !StringUtils.hasText(form.getDepartment())) {
            throw new IllegalArgumentException("doctor name and department cannot be blank");
        }
    }

    private void ensureDoctorNotExists(String department, String doctorName) {
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department)
            .eq(DoctorSchedule::getDoctorName, doctorName);
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new DuplicateKeyException("doctor already exists");
        }
    }

    private void ensureDoctorNameAvailable(List<DoctorSchedule> currentRows, String department, String doctorName) {
        if (!currentRows.isEmpty()
            && department.equals(currentRows.get(0).getDepartment())
            && doctorName.equals(currentRows.get(0).getDoctorName())) {
            return;
        }
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department)
            .eq(DoctorSchedule::getDoctorName, doctorName);
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new DuplicateKeyException("doctor already exists");
        }
    }

    private DoctorSchedule requireSchedule(Long id) {
        DoctorSchedule schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("doctor schedule not found: " + id);
        }
        return schedule;
    }

    private DoctorDutySummary findDoctorSummary(String department, String doctorName) {
        List<DoctorSchedule> schedules = listSchedulesByDoctor(department, doctorName);
        if (schedules.isEmpty()) {
            return null;
        }
        return toSummary(schedules);
    }
}
