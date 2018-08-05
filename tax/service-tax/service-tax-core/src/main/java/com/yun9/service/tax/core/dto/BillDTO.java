package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillDTO {
    /**
     * 票据类型[output自开][agent代开][nobill无票][income进项]',
     */
    String billType;
    /**
     * 开票类型[service服务][cargo劳务]
     */
    String category;
    /**
     * 发票类型[special专票][plain普票]
     */
    String type;
    Long cycleId;
    BigDecimal taxRate;
    BigDecimal declareAmount;
    String cycle;

}
