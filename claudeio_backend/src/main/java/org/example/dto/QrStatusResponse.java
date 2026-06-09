package org.example.dto;

/**
 * 二维码状态响应
 */
public class QrStatusResponse {
    private int code;           // 状态码: 800/801/802/803
    private String message;     // 状态描述
    private String cookie;      // Cookie（仅 803 成功时有）

    public QrStatusResponse(int code, String message, String cookie) {
        this.code = code;
        this.message = message;
        this.cookie = cookie;
    }

    public static QrStatusResponse expired() {
        return new QrStatusResponse(800, "二维码已过期", null);
    }

    public static QrStatusResponse waiting() {
        return new QrStatusResponse(801, "等待扫码", null);
    }

    public static QrStatusResponse scanned() {
        return new QrStatusResponse(802, "已扫码，等待确认", null);
    }

    public static QrStatusResponse success(String cookie) {
        return new QrStatusResponse(803, "登录成功", cookie);
    }

    // Getters
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getCookie() {
        return cookie;
    }

    public boolean isSuccess() {
        return code == 803;
    }
}
