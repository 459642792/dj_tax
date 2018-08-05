package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BizTaxVatImportSheetDTO implements Serializable {
    private byte[] fileData;
    private String fileUploadPath;
    private String fileOriginalName;
    private String processBy;
    private long mdAccountCycleId;
    private long instId;
    private long userId;
}
