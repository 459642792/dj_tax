package com.yun9.service.tax.domain.dto;

import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.enums.TaxOffice;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-01 16:37
 */
@Data
public class TaxInstanceDTO implements Serializable {

    private Long bizTaxInstanceId;

    private Long mdCompanyId;
    private String mdClientSn; //公司ID
    private String passwordType;

    private String mdCompanyName;//申报月份
    private Long mdAreaId;
    private TaxOffice taxOffice;
    private String mdCompanyTaxType;
    private String taxCategories;

    private BigDecimal totalAmount;
//    private BizTaxInstance.DownloadState downloadState;
    private String downloadRemark;
    private TaskDTO taskDTO;

    @Data
    public static class TaxCategories{
        private String taxSn;
        private TaxOffice taxOffice;
        private String declareState;
    }
}
