package org.example.dto;

/**
 * 网易云 Cookie 响应 DTO
 */
public class NeteaseCookieResponse {
    private boolean success;
    private String cookie;
    private String errorMessage;

    public NeteaseCookieResponse(boolean success, String cookie, String errorMessage) {
        this.success = success;
        this.cookie = cookie;
        this.errorMessage = errorMessage;
    }

    public static NeteaseCookieResponse success(String cookie) {
        return new NeteaseCookieResponse(true, cookie, null);
    }

    public static NeteaseCookieResponse failure(String errorMessage) {
        return new NeteaseCookieResponse(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCookie() {
        return cookie;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
