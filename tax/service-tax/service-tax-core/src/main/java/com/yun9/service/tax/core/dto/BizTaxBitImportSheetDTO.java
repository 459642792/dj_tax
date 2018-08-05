package com.yun9.service.tax.core.dto;

import com.yun9.biz.tax.enums.TaxOffice;
import lombok.Data;

/**
 * @author huanglei
 * @version 1.0
 * @Description: TODO
 * @since 2018-06-02 16:46
 */
@Data
public class BizTaxBitImportSheetDTO {
    private byte[] fileData;
    private String fileUploadPath;
    private String fileOriginalName;
    private String processBy;
    private long mdAccountCycleId;
    private long userId;
    private long instId;
    private TaxOffice taxOffice;
}
