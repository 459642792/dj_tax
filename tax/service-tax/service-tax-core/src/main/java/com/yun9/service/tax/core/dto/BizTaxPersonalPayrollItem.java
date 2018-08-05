package com.yun9.service.tax.core.dto;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import java.util.List;
import lombok.Data;

/**
 * @author huanglei
 * @version 1.0
 * @Description: TODO
 * @since 2018-06-01 15:17
 */
@Data
public class BizTaxPersonalPayrollItem extends BizTaxInstanceCategoryPersonalPayrollItem {
    private int code; //状态
    private List message;//说明
}
