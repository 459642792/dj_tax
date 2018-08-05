package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 个税工资薪金历史申报
 * @Author: chenbin
 * @Date: 2018-06-01
 * @Time: 11:39
 * @Description:
 */
@Data
public class PersonalHistoryPayrollDTO implements Serializable{
    private Long id;
    private Long cycleId;
    private String cycle;
    private Integer popleNum = 0;
    private BigDecimal taxAmount = new BigDecimal(0.00);
}
