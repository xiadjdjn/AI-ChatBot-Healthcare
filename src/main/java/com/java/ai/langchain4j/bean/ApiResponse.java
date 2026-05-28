package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用接口返回对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
     //业务码。
    private Integer code;

     // 返回消息。
    private String message;

     //返回数据。
    private T data;

    /**
     * 构建成功返回。
     *
     * @param data 返回数据
     * @return 通用返回对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /**
     * 构建指定消息的成功返回。
     *
     * @param message 返回消息
     * @param data 返回数据
     * @return 通用返回对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }
}
