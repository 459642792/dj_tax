package com.yun9.service.tax.domain.dto;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.TaxOffice;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class MultiScreenShotTaskDTO implements Serializable {

    private Long userId;

    @NotNull
    private BizTaxInstanceCategory.DeclareCheckState declareCheckState;
    @NotNull
    private TaxOffice taxOffice;
    @NotNull
    private long mdAccountCycleId;
    private int limit = 0;
    private List<Long> taxInstanceCategoryIds;
}
