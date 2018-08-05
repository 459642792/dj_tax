package com.yun9.service.tax.core.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class ExcelDTO {
    /**
     * 列字段
     */
    String filed;
    /**
     * 列名称
     */
    @NonNull
    String name;
    /**
     * 批注
     */
    String comment;
    /**
     * 背景色
     */
    Short backgroundColor;
    /**
     * 字体大小
     */
    @NonNull
    Short fontHeightInPoints;
    /**
     * 字体颜色
     */
    Short fontColor;
    /**
     * 行高
     */
    Short rowHeight;

    public ExcelDTO() {
    }
}
