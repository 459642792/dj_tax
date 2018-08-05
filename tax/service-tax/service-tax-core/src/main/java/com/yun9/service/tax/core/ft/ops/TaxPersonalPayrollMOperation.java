package com.yun9.service.tax.core.ft.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.BizTaxInstanceService;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollItemFactory;
import com.yun9.service.tax.core.dto.BizTaxInstanceCategoryPersonalPayrollItemStateDTO;
import com.yun9.service.tax.core.ft.TaxCategoryMapping;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-08 14:46
 */
@TaxCategoryMapping(sn = {TaxSn.m_personal_payroll})
public class TaxPersonalPayrollMOperation implements AuditHandler {

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    TaxInstanceCategoryPersonalPayrollItemFactory taxInstanceCategoryPersonalPayrollItemFactory;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

    @Autowired
    BizTaxInstanceService bizTaxInstanceService;

    @Override
    public boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory) {
        if (null == bizTaxInstanceCategory) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "税种实例不能为空");
        }
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategory.getId()))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "个税工资薪金不存在"));
        if (bizTaxInstanceCategoryPersonalPayroll.getDisabled() != 0) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "税种状态只能为可用状态");
        }
        //检查申报类型
        if (BizTaxInstanceCategoryPersonalPayroll.SourceType.last
                .equals(bizTaxInstanceCategoryPersonalPayroll.getSourceType())) {
            return true;
        }
        return false;
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory) {
//        if (null == bizTaxInstanceCategory) {
//            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "税种实例不能为空");
//        }
//
//        if (BizTaxInstanceCategory.ProcessState.exception.equals(bizTaxInstanceCategory.getProcessState()) ||
//                BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())){
//            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "审核失败,税种实例处于异常或办理状态!");
//        }

        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategory.getId()))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "个税工资薪金不存在"));
        if (bizTaxInstanceCategoryPersonalPayroll.getDisabled() != 0) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "税种状态只能为可用状态");
        }
        //按上月无需校验人员清单
        if ( BizTaxInstanceCategoryPersonalPayroll.SourceType.last.equals(bizTaxInstanceCategoryPersonalPayroll.getSourceType())){
            return;
        }
        //获取实例
        //获取公司id和机构id
        // BizTaxInstance bizTaxInstance = bizTaxInstanceService.findByTaxId(bizTaxInstanceCategoryPersonalPayroll.getId(), TaxSn.m_personal_payroll);
        BizTaxInstance bizTaxInstance = bizTaxInstanceService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategoryPersonalPayroll.getId());
        if (null != bizTaxInstance) {
            long mdCompanyId = bizTaxInstance.getMdCompanyId();
            long mdClientId = bizTaxInstance.getMdInstClientId();
            //获取人员清单
            List<BizTaxInstanceCategoryPersonalPayrollItem> payrollList = bizTaxInstanceCategoryPersonalPayrollItemService.findByBizTaxInstanceCategoryPersonalPayrollIdAndUseType(bizTaxInstanceCategoryPersonalPayroll.getId(), BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare);
            //校验
            if (payrollList.size() > 0) {
                payrollList.forEach(v -> {
                    BizTaxInstanceCategoryPersonalPayrollItemStateDTO payrollItemStateDTO =
                            taxInstanceCategoryPersonalPayrollItemFactory.vaild(v, mdCompanyId, mdClientId);
                    int code = payrollItemStateDTO.getCode();
                    if (code == 1) {
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "人員清單数据校验未通过,错误类型:" + payrollItemStateDTO.getMessage());
                    } else if (code == 2) {
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "人員清單数据校验未通过,数据重复:");
                    }
                });
            } else {
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "按实际申报,校验人员清单,未找到人员清单信息");
            }
        } else {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到税种实例");
        }
    }

    @Override
    public void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory) {

    }
}
