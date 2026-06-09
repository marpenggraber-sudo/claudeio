package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 网易云登录请求 DTO
 */
public record NeteaseLoginRequest(

    @NotBlank(message = "登录类型不能为空")
    @Pattern(regexp = "phone|email", message = "登录类型只能是 phone 或 email")
    String loginType,

    @NotBlank(message = "账号不能为空")
    String account,

    @NotBlank(message = "密码不能为空")
    String password
) {
}
