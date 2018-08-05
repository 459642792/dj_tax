package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.AreaSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.AbstractTaxRouterBuilder;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-13 14:31
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_personal_payroll_by_last_month, taxOffice = TaxOffice.ds, cycleType = CycleType.m,area = AreaSn.shenzhen)
}
)
public class SendPersonalPayrollOperationByLastMonth extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private final static Logger logger = LoggerFactory.getLogger(SendPersonalPayrollOperationByLastMonth.class);

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    
    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    TaskSendHandler taskSendHandler;

    @Autowired
    BizTaxDeclareService bizTaxDeclareService;
    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
       
        return new HashMap<String, Object>(5){{
            logger.debug("开始构建税务路由参数：");
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getMonth());
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
            put("taxclosingdate",context.getBizTaxInstanceCategory().getCloseDate() * 1000);
            
            logger.debug("完成参数构建-->{}", this);
        }};
        
    }

    @Override
    public Object process(OperationContext context) {
        //================申报实例申报前状态校验==========================
        if (!context.getBizTaxInstanceCategory().prepare2Send()) {
            logger.error("申报状态错误",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(),context.getBizTaxInstanceCategory().isAudit()?"已审核":"未审核"));
        }
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(
                bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(context.getBizTaxInstanceCategory().getId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该个体工资薪金申报记录"));
        if ( !BizTaxInstanceCategoryPersonalPayroll.SourceType.last.equals(bizTaxInstanceCategoryPersonalPayroll.getSourceType())){
            throw ServiceTaxException.build(ServiceTaxException.Codes.NotSupportTask,"当前个体工资薪金申报类型不是按上月");
        }
        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));
        //====================调用申报发起成功========================================================
        bizTaxDeclareService.startDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        return "任务发起成功";
        
    }
}
