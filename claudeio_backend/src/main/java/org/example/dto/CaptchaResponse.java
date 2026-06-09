package org.example.dto;

/**
 * 验证码响应 DTO
 */
public class CaptchaResponse {
    private boolean success;
    private String message;

    public CaptchaResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static CaptchaResponse success(String message) {
        return new CaptchaResponse(true, message);
    }

    public static CaptchaResponse failure(String message) {
        return new CaptchaResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
