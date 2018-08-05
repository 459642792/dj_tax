package com.yun9.service.tax.core.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @author huanglei
 * @version 1.0
 * @Description: TODO
 * @since 2018-06-02 16:55
 */
@Data
public class BizTaxBitImportDTO {
    List<BizTaxBitItemDTO> auditSheet;

    public BizTaxBitImportDTO() {
        this.auditSheet =new ArrayList<>();
    }

}
