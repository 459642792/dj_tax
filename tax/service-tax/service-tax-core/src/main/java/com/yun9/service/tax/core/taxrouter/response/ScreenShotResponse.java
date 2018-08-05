package com.yun9.service.tax.core.taxrouter.response;

import lombok.Data;

import java.util.List;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-04-24 9:58
 */
@Data
public class ScreenShotResponse {
    private String taxCode;
    private List<FileDTO> picList;

    @Data
    public static class FileDTO {
        private String type;
        private String fileType;
        private String url;
    }
}
