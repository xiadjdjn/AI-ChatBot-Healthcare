package com.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.java.ai.langchain4j.bean.ChangePasswordForm;
import com.java.ai.langchain4j.bean.LoginForm;
import com.java.ai.langchain4j.bean.LoginResult;
import com.java.ai.langchain4j.bean.RegisterForm;
import com.java.ai.langchain4j.bean.UserProfileSummary;
import com.java.ai.langchain4j.entity.User;
import com.java.ai.langchain4j.mapper.UserMapper;
import com.java.ai.langchain4j.service.UserService;
import com.java.ai.langchain4j.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 校验用户名和密码，并生成 JWT。
     *
     * @param loginForm 登录请求参数
     * @return 登录成功返回信息
     */
    @Override
    public LoginResult login(LoginForm loginForm) {
        if (loginForm == null || !StringUtils.hasText(loginForm.getUsername()) || !StringUtils.hasText(loginForm.getPassword())) {
            throw new IllegalArgumentException("username and password cannot be blank");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginForm.getUsername());
        queryWrapper.eq(User::getEnabled, 1);
        User user = baseMapper.selectOne(queryWrapper);
        if (user == null || !passwordEncoder.matches(loginForm.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("invalid username or password");
        }

        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : "user";
        user.setRole(role);
        return new LoginResult(jwtUtil.generateToken(user), user.getId(), user.getUsername(), user.getNickname(), role, "admin".equalsIgnoreCase(role));
    }

    /**
     * 注册普通用户并生成 JWT。
     *
     * @param registerForm 注册请求参数
     * @return 注册成功返回信息
     */
    @Override
    public LoginResult register(RegisterForm registerForm) {
        if (registerForm == null
            || !StringUtils.hasText(registerForm.getUsername())
            || !StringUtils.hasText(registerForm.getPassword())
            || !StringUtils.hasText(registerForm.getConfirmPassword())) {
            throw new IllegalArgumentException("username and password cannot be blank");
        }
        if (!registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            throw new IllegalArgumentException("password and confirm password do not match");
        }
        if (registerForm.getPassword().length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }

        String username = registerForm.getUsername().trim();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new IllegalArgumentException("username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerForm.getPassword()));
        user.setNickname(StringUtils.hasText(registerForm.getNickname()) ? registerForm.getNickname().trim() : username);
        user.setRole("user");
        user.setEnabled(1);
        baseMapper.insert(user);
        return new LoginResult(jwtUtil.generateToken(user), user.getId(), user.getUsername(), user.getNickname(), user.getRole(), false);
    }

    @Override
    public UserProfileSummary getProfile(Long userId) {
        User user = requireUser(userId);
        return new UserProfileSummary(user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public void changePassword(Long userId, ChangePasswordForm form) {
        if (form == null) {
            throw new IllegalArgumentException("change password form cannot be null");
        }
        if (!StringUtils.hasText(form.getOldPassword())
            || !StringUtils.hasText(form.getNewPassword())
            || !StringUtils.hasText(form.getConfirmPassword())) {
            throw new IllegalArgumentException("password fields cannot be blank");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("new password and confirm password do not match");
        }
        if (form.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("new password must be at least 6 characters");
        }

        User user = requireUser(userId);
        if (!passwordEncoder.matches(form.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("old password is incorrect");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("new password cannot be the same as old password");
        }

        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        baseMapper.updateById(user);
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        User user = baseMapper.selectById(userId);
        if (user == null || user.getEnabled() == null || user.getEnabled() != 1) {
            throw new IllegalArgumentException("user does not exist or has been disabled");
        }
        return user;
    }
}
