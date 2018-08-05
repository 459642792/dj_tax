package com.yun9.service.tax.core.v2.target;

import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.enums.CycleType;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.v2.ITargetExecutor;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.task_start_faied;


/**
 * 发送税务路由请求到任务中心
 */
@Component("target_task3")
public class ToTaskFromTaxRouterExecutor3 extends AbstractExecutor implements ITargetExecutor {

    public static final Logger logger = LoggerFactory.getLogger(ToTaskFromTaxRouterExecutor3.class);

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;
    @Override
    public Object execute(OperationContext context) {
        logger.info("全局请求信息-->{}", context.getRequest());

        //如果会计区间不存在(同步客户资料时)，默认设置当前月份会计区间
        if(null == context.getRequest().getAccountCycleId()){
            BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findByCurrentAndType(CycleType.m);
            context.setBizMdAccountCycle(bizMdAccountCycle);
            context.hasAccountCycle().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[会计期间不存在]"));
        }else{
            this.initAccountCycle(context).hasAccountCycle().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[会计期间不存在]"));
        }
        //初始化基础信息&& 验证上下文
        this.initInst(context).hasInst().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[机构不存在]"));
        this.initCompany(context).hasCompany().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[公司不存在]"));
        this.initArea(context).hasArea().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[税区不存在]"));

        this.initInstClient(context).hasInstClient().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[机构客户不正确]"));
        TaskStartHandler2 taskStartHandler = taskStartHandler2(context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType(), context.getBizMdArea().getSn()); //开始任务
        if(null == taskStartHandler){
            throw ServiceTaxException.build(task_start_faied, "没有找到" + String.format("actionSn->%s,taxOffice->%s,cycleType->%s,area->%s", context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType(), context.getBizMdArea().getSn()) + "任务处理器");
        }
        logger.info("全局请求信息--参数组装完成");
        taskStartHandler.process(context);
        return new HashMap() {{
            put("message", "发送成功");
        }};
    }


}
