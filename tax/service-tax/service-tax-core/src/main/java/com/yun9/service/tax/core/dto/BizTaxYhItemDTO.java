package com.yun9.service.tax.core.dto;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYhItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-14 10:54
 */
@Data
public class BizTaxYhItemDTO implements Serializable {
    private long yhId;
    List<BizTaxInstanceCategoryYhItem> yhItems;
}
