package com.yun9.service.tax.core.v2;

import com.yun9.service.tax.core.v2.annotation.ActionSn;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class MultOperationRequest {

    private Long userId;
    private Map<String, Object> params;
    @NotNull
    private ActionSn actionSn;

    private List<Long> taxInstanceCategoryIds;

}
