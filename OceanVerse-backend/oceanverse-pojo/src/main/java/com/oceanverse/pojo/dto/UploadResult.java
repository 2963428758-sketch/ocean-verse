package com.oceanverse.pojo.dto;

import lombok.Data;

@Data
public class UploadResult {
    private String url;
    private String fileName;
    private Long fileSize;

    public static UploadResult of(String url, String fileName, Long fileSize) {
        UploadResult result = new UploadResult();
        result.setUrl(url);
        result.setFileName(fileName);
        result.setFileSize(fileSize);
        return result;
    }
}
