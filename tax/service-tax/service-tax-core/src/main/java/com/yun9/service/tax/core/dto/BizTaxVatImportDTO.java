package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析上下文
 */
@Data
public class BizTaxVatImportDTO {


    Map<Integer, List<BizTaxBillInvoiceSheetDTO>> sheets;

    public BizTaxVatImportDTO() {
        this.sheets = new HashMap<>();
    }

}
