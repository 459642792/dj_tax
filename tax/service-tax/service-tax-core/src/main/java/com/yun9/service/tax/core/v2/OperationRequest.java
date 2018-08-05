package com.yun9.service.tax.core.v2;

import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class OperationRequest {


    private Long instId;
    private Long companyId;
    private CycleType cycleType;
    private Long accountCycleId;
    private TaxOffice taxOffice;

    private Long taxInstanceCategoryId;
    private Map<String, Object> params;

    //    @NotNull
    private Long userId;

    @NotNull
    private ActionSn actionSn;

}
