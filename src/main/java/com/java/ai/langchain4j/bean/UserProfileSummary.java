package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户个人信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileSummary {

    private Long userId;

    private String username;

    private String nickname;
}
