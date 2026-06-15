package com.java.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.java.ai.langchain4j.bean.ChangePasswordForm;
import com.java.ai.langchain4j.bean.LoginForm;
import com.java.ai.langchain4j.bean.LoginResult;
import com.java.ai.langchain4j.bean.RegisterForm;
import com.java.ai.langchain4j.bean.UserProfileSummary;
import com.java.ai.langchain4j.entity.User;

public interface UserService extends IService<User> {

    /**
     * 校验用户名和密码，并生成 JWT。
     *
     * @param loginForm 登录请求参数
     * @return 登录成功返回信息
     */
    LoginResult login(LoginForm loginForm);

    /**
     * 注册普通用户并生成 JWT。
     *
     * @param registerForm 注册请求参数
     * @return 注册成功返回信息
     */
    LoginResult register(RegisterForm registerForm);

    /**
     * 查询当前登录用户资料。
     *
     * @param userId 用户 ID
     * @return 个人资料
     */
    UserProfileSummary getProfile(Long userId);

    /**
     * 修改当前登录用户密码。
     *
     * @param userId 用户 ID
     * @param form 修改密码表单
     */
    void changePassword(Long userId, ChangePasswordForm form);
}
