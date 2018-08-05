package com.yun9.service.tax.core.v2.target;

import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.v2.ITargetExecutor;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.task_start_faied;


/**
 * 发送税务路由请求到任务中心
 */
@Component("target_task2")
public class ToTaskFromTaxRouterExecutor2 extends AbstractExecutor implements ITargetExecutor {

    public static final Logger logger = LoggerFactory.getLogger(ToTaskFromTaxRouterExecutor2.class);
    private final static Set<ActionSn> EX_VALIDATE_SN = new HashSet<ActionSn>();

    static {
        EX_VALIDATE_SN.add(ActionSn.get_taxes);
    }

    @Autowired
    protected BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Override
    public Object execute(OperationContext context) {
        logger.info("全局请求信息-->{}", context.getRequest());
        if (!EX_VALIDATE_SN.contains(context.getRequest().getActionSn())) {
            if (null == context.getRequest().getTaxInstanceCategoryId() || 0 == context.getRequest().getTaxInstanceCategoryId()) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "申报记录id不能为空");
            }
        }

        if (null != context.getRequest().getTaxInstanceCategoryId() && 0 != context.getRequest().getTaxInstanceCategoryId()) {
            BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(context.getRequest().getTaxInstanceCategoryId());
            if (null != bizTaxInstanceCategory && null != bizTaxInstanceCategory.getBizTaxInstance()) {
                context.setBizTaxInstance(bizTaxInstanceCategory.getBizTaxInstance());
                context.setBizTaxInstanceCategory(bizTaxInstanceCategory);
                context.getRequest().setAccountCycleId(bizTaxInstanceCategory.getBizTaxInstance().getMdAccountCycleId());
                context.getRequest().setCompanyId(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId());
                context.getRequest().setTaxOffice(bizTaxInstanceCategory.getBizTaxInstance().getTaxOffice());
                context.getRequest().setCycleType(bizTaxInstanceCategory.getCycleType());
                BizMdInstClient bizMdInstClient = bizMdInstClientService.findById(bizTaxInstanceCategory.getBizTaxInstance().getMdInstClientId());
                if (null != bizMdInstClient) {
                    context.getRequest().setInstId(bizMdInstClient.getBizMdInstId());
                    context.setBizMdInstClient(bizMdInstClient);
                }
            }
        }
        //初始化&& 验证上下文
        this.initInst(context).hasInst().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[机构不存在]"));
        this.initCompany(context).hasCompany().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[公司不存在]"));
        this.initAccountCycle(context).hasAccountCycle().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[会计期间不存在]"));
        this.initArea(context).hasArea().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[税区不存在]"));
        this.initInstClient(context).hasInstClient().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[机构客户不正确]"));
        logger.info("开始获取actionSn->{},taxOffice->{},cycleType->{} 对应的任务处理器", context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType());
        TaskStartHandler2 taskStartHandler = taskStartHandler2(context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType(), context.getBizMdArea().getSn()); //开始任务
        if (taskStartHandler == null) {
            throw ServiceTaxException.build(task_start_faied, "没有找到" + String.format("actionSn->%s,taxOffice->%s,cycleType->%s,area->%s", context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType(), context.getBizMdArea().getSn()) + "任务处理器");
        }
        logger.info("获取到任务处理器-->{}", taskStartHandler.getClass().getName());

        taskStartHandler.process(context);

        return new HashMap() {{
            put("message", "发送成功");
        }};
    }


}
