package com.java.ai.langchain4j.tools;

import com.java.ai.langchain4j.entity.Appointment;
import com.java.ai.langchain4j.entity.DoctorSchedule;
import com.java.ai.langchain4j.service.AppointmentService;
import com.java.ai.langchain4j.service.DoctorScheduleService;
import com.java.ai.langchain4j.util.UserContextHolder;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AppointmentTools {
    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Tool(name="预约挂号", value = "根据参数，先执行工具方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约。如果用户没有提供具体的医生姓名，请从" +
        "向量存储中找到一位医生。")
    public String bookAppointment(Appointment appointment){
        bindCurrentUser(appointment);
        appointment.setTime(normalizeTime(appointment.getTime()));
        appointment.setStatus(AppointmentService.STATUS_IN_PROGRESS);
        appointment.setUpdatedAt(LocalDateTime.now());
        DoctorSchedule schedule = doctorScheduleService.findAvailableSchedule(
            appointment.getDepartment(),
            appointment.getDate(),
            appointment.getTime(),
            appointment.getDoctorName()
        );
        if (schedule == null) {
            return "当前科室和时间暂无可预约号源";
        }
        if (appointment.getDoctorName() == null || appointment.getDoctorName().isBlank()) {
            appointment.setDoctorName(schedule.getDoctorName());
        }
        //查找数据库中是否包含对应的预约记录
        Appointment appointmentDB = appointmentService.getOne(appointment);
            if(appointmentDB == null){
            appointment.setId(null);//防止大模型幻觉设置了id
            if(appointmentService.save(appointment)){
                return "预约成功，并返回预约详情";
            }else{
                return "预约失败";
            }
        }
        return "您在相同的科室和时间已有预约";
    }
    @Tool(name="取消预约挂号", value = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
    public String cancelAppointment(Appointment appointment){
    bindCurrentUser(appointment);
    Appointment appointmentDB = appointmentService.getOne(appointment);
        if(appointmentDB != null){
            //取消预约时保留记录，仅更新状态。
            appointmentDB.setStatus(AppointmentService.STATUS_CANCELLED);
            appointmentDB.setUpdatedAt(LocalDateTime.now());
            if(appointmentService.updateById(appointmentDB)){
            return "取消预约成功";
            }else{
                return "取消预约失败";
            }
        }
        //取消失败
        return "您没有预约记录，请核对预约科室和时间";
    }
    @Tool(name = "查询是否有号源", value="根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
    public boolean queryDepartment(
        @P(value = "科室名称") String name,
        @P(value = "日期") String date,
        @P(value = "时间，可选值：上午、下午，也可以是10点、10:00这类具体时间") String time,
        @P(value = "医生名称", required = false) String doctorName
        ) {
            String normalizedTime = normalizeTime(time);
            System.out.println("查询是否有号源");
            System.out.println("科室名称：" + name);
            System.out.println("日期：" + date);
            System.out.println("时间：" + normalizedTime);
            System.out.println("医生名称：" + doctorName);
            //根据医生排班和已预约数量判断是否还有可预约号源。
            return doctorScheduleService.hasAvailableSchedule(name, date, normalizedTime, doctorName);
    }

    private void bindCurrentUser(Appointment appointment) {
        Long currentUserId = UserContextHolder.getUserId();
        String currentUsername = UserContextHolder.getUsername();
        if (currentUserId != null) {
            appointment.setUserId(currentUserId);
        }
        if (StringUtils.hasText(currentUsername)) {
            appointment.setAccountUsername(currentUsername);
        }
    }

    /**
     * 将具体时间归一化为上午或下午。
     *
     * @param time 用户传入的时间
     * @return 上午或下午
     */
    private String normalizeTime(String time) {
        if (!StringUtils.hasText(time)) {
            return time;
        }
        if (time.contains("上午") || time.contains("早上") || time.contains("早晨")) {
            return "上午";
        }
        if (time.contains("下午") || time.contains("中午") || time.contains("晚上")) {
            return "下午";
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})(点|:)").matcher(time);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            return hour < 12 ? "上午" : "下午";
        }
        return time;
    }
}
