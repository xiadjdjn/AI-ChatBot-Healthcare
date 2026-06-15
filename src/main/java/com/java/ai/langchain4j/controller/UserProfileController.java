package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.bean.ChangePasswordForm;
import com.java.ai.langchain4j.bean.UserAppointmentSummary;
import com.java.ai.langchain4j.bean.UserProfileSummary;
import com.java.ai.langchain4j.service.AppointmentService;
import com.java.ai.langchain4j.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前登录用户个人中心接口。
 */
@Tag(name = "User Profile")
@RestController
@RequestMapping("/users/me")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Operation(summary = "获取当前用户资料")
    @GetMapping
    public ApiResponse<UserProfileSummary> profile(HttpServletRequest request) {
        return ApiResponse.success(userService.getProfile(currentUserId(request)));
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/password")
    public ApiResponse<Boolean> changePassword(@RequestBody ChangePasswordForm form,
                                               HttpServletRequest request) {
        userService.changePassword(currentUserId(request), form);
        return ApiResponse.success("password updated", true);
    }

    @Operation(summary = "查询当前用户预约记录")
    @GetMapping("/appointments")
    public ApiResponse<List<UserAppointmentSummary>> appointments(HttpServletRequest request) {
        return ApiResponse.success(appointmentService.listCurrentUserAppointments(currentUserId(request), currentUsername(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long currentUserId) {
            return currentUserId;
        }
        throw new IllegalArgumentException("current user is not authenticated");
    }

    private String currentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username == null ? null : String.valueOf(username);
    }
}
