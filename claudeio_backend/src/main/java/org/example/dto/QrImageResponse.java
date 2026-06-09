package org.example.dto;

/**
 * 二维码图片响应
 */
public class QrImageResponse {
    private boolean success;
    private String qrurl;      // 二维码链接
    private String qrimg;      // base64 图片
    private String errorMessage;

    public QrImageResponse(boolean success, String qrurl, String qrimg, String errorMessage) {
        this.success = success;
        this.qrurl = qrurl;
        this.qrimg = qrimg;
        this.errorMessage = errorMessage;
    }

    public static QrImageResponse success(String qrurl, String qrimg) {
        return new QrImageResponse(true, qrurl, qrimg, null);
    }

    public static QrImageResponse failure(String errorMessage) {
        return new QrImageResponse(false, null, null, errorMessage);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getQrurl() {
        return qrurl;
    }

    public String getQrimg() {
        return qrimg;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
