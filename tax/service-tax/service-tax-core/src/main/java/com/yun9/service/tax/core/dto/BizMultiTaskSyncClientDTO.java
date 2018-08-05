package com.yun9.service.tax.core.dto;

import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class BizMultiTaskSyncClientDTO implements Serializable {

    private Long instId;  // 可以指定机构，可以不指定
    private long userId;
    private ActionSn actionSn;
    private TaxOffice taxOffice;
    private long bizMdAccountId; // 根据跟激活的登录方式

    private CycleType cycleType;
    private Long accountCycleId;
    private Integer limit; // 限制任务数
    private Set<Long> excludeInsts;

}
