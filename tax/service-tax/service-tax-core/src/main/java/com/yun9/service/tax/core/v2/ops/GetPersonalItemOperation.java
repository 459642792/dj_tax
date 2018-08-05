package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalMFactory;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
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
 * 下载个税人员清单
 *
 * @Author: chenbin
 * @Date: 2018-06-05
 * @Time: 15:25
 * @Description:
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.get_personal_item_m, taxOffice = TaxOffice.ds, cycleType = CycleType.m)
}
)
public class GetPersonalItemOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {
    private final static Logger logger = LoggerFactory.getLogger(GetPersonalItemOperation.class);

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxDeclareService bizTaxDeclareService;

    @Autowired
    TaxInstanceCategoryPersonalMFactory taxInstanceCategoryPersonalMFactory;

    @Autowired
    TaskSendHandler taskSendHandler;

    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        return new HashMap<String, Object>(4) {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getMonth());
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
        }};
    }

    @Override
    public Object process(OperationContext context) {
        //===================下载前校验======================
        logger.debug("----开始进行申报前的状态检查---");
        Map<String, Object> checkBackMap = taxInstanceCategoryPersonalMFactory.downItemFromTaxCheck(context.getBizTaxInstanceCategory().getId());
        int checkCode = Integer.parseInt(checkBackMap.get("code") + "");
        if (checkCode != 0) {
            String errMsg = checkBackMap.get("msg").toString();
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskStartFailed, errMsg);
        }
        //===================下载前校验======================

        //=====================发起任务==============================================
        logger.debug("开始发起任务");
        taskSendHandler.send(context, super.build(context));

        //====================创建下载记录========================================================
        logger.debug("开始创建下载记录");
        bizTaxDeclareService.startBeforeDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);

        return "任务发起成功";
    }
}
