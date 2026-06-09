package org.example.dto;

/**
 * 二维码 Key 响应
 */
public class QrKeyResponse {
    private boolean success;
    private String unikey;
    private String errorMessage;

    public QrKeyResponse(boolean success, String unikey, String errorMessage) {
        this.success = success;
        this.unikey = unikey;
        this.errorMessage = errorMessage;
    }

    public static QrKeyResponse success(String unikey) {
        return new QrKeyResponse(true, unikey, null);
    }

    public static QrKeyResponse failure(String errorMessage) {
        return new QrKeyResponse(false, null, errorMessage);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getUnikey() {
        return unikey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
