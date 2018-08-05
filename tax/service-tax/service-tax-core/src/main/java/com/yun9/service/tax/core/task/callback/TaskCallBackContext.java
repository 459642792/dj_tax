package com.yun9.service.tax.core.task.callback;

import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceSeq;
import lombok.Data;

@Data
public class TaskCallBackContext {
    private TaskCallBackResponse taskCallBackResponse;
    private BizTaxInstance bizTaxInstance;
    private BizTaxInstanceSeq bizTaxInstanceSeq;
    private BizTaxInstanceCategory bizTaxInstanceCategory;
    private BizTaxCompanyTax bizTaxCompanyTax;
    private String body;
    private String createBy;
}
