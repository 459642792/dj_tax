package com.yun9.service.tax.core.event;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import lombok.Data;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-07-10 17:39
 */
@Data
public class TaxDeclaredEvent extends ServiceTaxEvent {
    private BizTaxInstanceCategory bizTaxInstanceCategory;
}
