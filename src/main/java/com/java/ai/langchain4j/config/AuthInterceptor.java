package com.java.ai.langchain4j.config;

import com.java.ai.langchain4j.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 请求鉴权拦截器。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    private static final String ROLE_ADMIN = "admin";

    /**
     * 校验请求头中的 Bearer Token。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return true 表示放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"missing token\",\"data\":false}");
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(authorization.substring(7));
            String role = claims.get("role", String.class);
            String requestPath = resolveRequestPath(request);
            if (!ROLE_ADMIN.equalsIgnoreCase(role) && !isUserAllowedPath(requestPath)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"admin role required\",\"data\":false}");
                return false;
            }
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("role", role);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"invalid token\",\"data\":false}");
            return false;
        }
    }

    /**
     * 解析去掉应用上下文后的请求路径。
     *
     * @param request HTTP 请求
     * @return 请求路径
     */
    private String resolveRequestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    /**
     * 判断普通用户是否允许访问当前路径。
     *
     * @param requestPath 请求路径
     * @return true 表示允许访问
     */
    private boolean isUserAllowedPath(String requestPath) {
        return requestPath.startsWith("/xiaoxiaobai/")
            || requestPath.equals("/users/me")
            || requestPath.startsWith("/users/me/")
            || requestPath.equals("/doctor-duties/current");
    }
}
