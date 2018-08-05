package com.yun9.service.tax.core.v2;

import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class OperationContext implements Serializable {


    private String taskSn;
    private String taskSeq;
    private String taskBizId;
    private long taskInstanceId;


    private OperationRequest request;

    private BizMdInstClient bizMdInstClient; //机构客户
    private BizMdInst bizMdInst; //机构
    private BizMdCompany bizMdCompany;//公司
    private BizMdAccountCycle bizMdAccountCycle; //会计区间
    private BizMdArea bizMdArea;//地区
    private CompanyAccountDTO defaultCompanyAccount;//用户 账号信息

    private BizTaxInstance bizTaxInstance;
    private BizTaxInstanceCategory bizTaxInstanceCategory;
    private Map<String, Object> otherParams = new HashMap<>();

    public OperationContext(){}


    public OperationContext(OperationRequest request) {
        this.request = request;
    }


    public Optional<BizMdCompany> hasCompany() {
        return Optional.ofNullable(this.bizMdCompany);
    }

    public Optional<BizMdInst> hasInst() {
        return Optional.ofNullable(this.bizMdInst);
    }

    public Optional<BizMdAccountCycle> hasAccountCycle() {
        return Optional.ofNullable(this.bizMdAccountCycle);
    }

    public Optional<BizMdArea> hasArea() {
        return Optional.ofNullable(this.bizMdArea);
    }

    public Optional<BizMdInstClient> hasInstClient() {
        return Optional.ofNullable(this.bizMdInstClient);
    }

    public Optional<BizTaxInstance> hasTaxInstance() {
        return Optional.ofNullable(this.bizTaxInstance);
    }

    public Optional<BizTaxInstanceCategory> hasTaxInstanceCategory() {
        return Optional.ofNullable(this.bizTaxInstanceCategory);
    }

}
