package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.util.List;

/**
 * @author yunjie
 * @version 1.0
 * @since 2018-05-30 16:53
 */
@Data
public class BizTaxInstanceCategoryPersonalPayrollItemStateDTO {

    private int code; //状态
    private long id; //当前人员Id
    private List message;//说明

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List getMessage() {
        return message;
    }

    public void setMessage(List message) {
        this.message = message;
    }
}
