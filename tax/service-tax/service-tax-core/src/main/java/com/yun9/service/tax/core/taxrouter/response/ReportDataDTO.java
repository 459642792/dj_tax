package com.yun9.service.tax.core.taxrouter.response;


import lombok.Data;

/**
 * 报表单元格数据
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-03-21 15:02
 */
@Data
public class ReportDataDTO {
    public enum DataType {
        str
    }

    /**
     * 唯一表示单元格key
     */
    private String code;
    /**
     * 单元格属性名称
     */
    private String name;
    /**
     * 单元格值
     */
    private String value;
    /**
     * 值的数据类型
     */
    private DataType dataType;
}
