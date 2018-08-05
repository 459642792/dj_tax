package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.util.List;

/**
 * @author wanghao
 * @since 2018-06-13 16:14
 * @version 1.0
 */
@Data
public class BizTaxInstanceCategoryYhItemDTO {

    private int code; //状态
    private List message;//说明

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List getMessage() {
        return message;
    }

    public void setMessage(List message) {
        this.message = message;
    }
}
