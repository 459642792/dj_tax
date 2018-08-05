package com.yun9.service.tax.core.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * @author huanglei
 * @version 1.0
 * @Description: TODO
 * @since 2018-05-31 20:12
 */
@Data
public class BizTaxPersonalImportSheetDTO implements Serializable {
    private byte[] fileData;
    private String fileUploadPath;
    private String fileOriginalName;
    private long mdAccountCycleId;
    private long categoryId;
    private long userId;
    private long mdCompanyId;
    private long mdInstClientId;
    private long taxareaId;
}
