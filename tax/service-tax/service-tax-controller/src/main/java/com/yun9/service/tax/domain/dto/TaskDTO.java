package com.yun9.service.tax.domain.dto;

import lombok.Data;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-02 14:55
 */
@Data
public class TaskDTO {
    private int code;
    private String message;

    public TaskDTO(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
