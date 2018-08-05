package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizSynScreenShotService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxException.Codes;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-28 16:23
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.syn_screenshot, taxOffice = TaxOffice.gs, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.syn_screenshot, taxOffice = TaxOffice.gs, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.syn_screenshot, taxOffice = TaxOffice.gs, cycleType = CycleType.q),
        @SnParameter(sn = ActionSn.syn_screenshot, taxOffice = TaxOffice.ds, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.syn_screenshot, taxOffice = TaxOffice.ds, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.syn_screenshot, taxOffice = TaxOffice.ds, cycleType = CycleType.q)
}
)
public class SynScreenShotOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private final static Logger logger = LoggerFactory.getLogger(SynScreenShotOperation.class);

    @Autowired
    private BizSynScreenShotService bizSynScreenShotService;
    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    private TaskSendHandler taskSendHandler;
    @Autowired
    private BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;

    @Override
    public Object process(OperationContext context) {
        //======================开始发起===============================

        taskSendHandler.send(context, super.defaultAccountAndParams(context));
        bizSynScreenShotService.startSynScreenShot(context.getBizTaxInstanceCategory(),
                context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        return "发起成功";
    }

    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        Map<Long, String> bizTaxMdOfficeCategoryMap = new HashMap<>();
        bizTaxMdOfficeCategoryService.findAll().forEach(v -> {
            bizTaxMdOfficeCategoryMap.put(v.getId(), v.getCode());
        });
        if (context.getRequest().getParams().get("synType")==null){
            throw ServiceTaxException.build(Codes.IllegalArgumentException,"synType不能为null");
        }
        SynType synType = SynType.valueOf(context.getRequest().getParams().get("synType").toString());
        List<String> taxCodes = new ArrayList<>();
        switch (synType) {
            case all:
                BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(context.getRequest().getTaxInstanceCategoryId());
                List<BizTaxInstanceCategory> bizTaxInstanceCategories = bizTaxInstanceCategoryService.findByBizTaxInstanceId(bizTaxInstanceCategory.getBizTaxInstanceId());
                bizTaxInstanceCategories.forEach(v -> {
                    taxCodes.add(bizTaxMdOfficeCategoryMap.get(v.getBizTaxMdOfficeCategoryId()));
                });
                break;
            case one:
                taxCodes.add(bizTaxMdOfficeCategoryMap.get(context.getBizTaxInstanceCategory().getBizTaxMdOfficeCategoryId()));
                break;
            default:
                throw ServiceTaxException.build(Codes.IllegalArgumentException, "同步截图参数类型错误");
        }
        return new HashMap() {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getCurrentMonth());
            put("cycleType", context.getRequest().getCycleType());
            put("taxCode", taxCodes.toArray());
            put("taxPaySn", context.getBizTaxInstanceCategory().getTaxPaySn());
        }};
    }

    private enum SynType {
        one, all;
    }




}
