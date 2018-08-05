package com.yun9.service.tax.core.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @author huanglei
 * @version 1.0
 * @Description: TODO
 * @since 2018-05-31 19:23
 */
@Data
public class BizTaxPersonalImportDTO  {
    List<BizTaxPersonalPayrollItem> singleSheet = new ArrayList<BizTaxPersonalPayrollItem>();
    List<BizTaxPersonalPayrollItem> repeatSheet = new ArrayList<BizTaxPersonalPayrollItem>();

}
