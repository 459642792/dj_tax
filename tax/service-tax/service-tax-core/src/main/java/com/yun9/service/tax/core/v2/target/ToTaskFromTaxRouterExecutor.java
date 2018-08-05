package com.yun9.service.tax.core.v2.target;

import com.yun9.biz.task.BizTaskService;
import com.yun9.biz.tax.BizTaxTaskPropertyService;
import com.yun9.commons.exception.BizException;
import com.yun9.service.tax.core.ServiceTaxProperties;
import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.ITargetExecutor;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler;
import com.yun9.service.tax.core.v2.handler.TaxRouterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


/**
 * 发送税务路由请求到任务中心
 */
@Component("target_task")
public class ToTaskFromTaxRouterExecutor extends AbstractExecutor implements ITargetExecutor {

    public static final Logger logger = LoggerFactory.getLogger(ToTaskFromTaxRouterExecutor.class);


    @Autowired
    BizTaskService bizTaskService;

    @Autowired
    BizTaxTaskPropertyService bizTaxTaskPropertyService;

    @Autowired
    ServiceTaxProperties serviceTaxProperties;

    @Autowired
    TaskSendHandler taskSendHandler;

    @Override
    public Object execute(OperationContext context) {

        //初始化&& 验证上下文
        this.initInst(context).hasInst().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[机构不存在]"));
        this.initCompany(context).hasCompany().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[公司不存在]"));
        this.initAccountCycle(context).hasAccountCycle().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[会计期间不存在]"));
        this.initArea(context).hasArea().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[税区不存在]"));
        this.initInstClient(context).hasInstClient().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("请求参数错误[机构客户不正确]"));

        TaskStartHandler taskStartHandler = taskStartHandler(context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType(), context.getBizMdArea().getSn()); //开始任务

        if (null == taskStartHandler) {
            throw new com.yun9.commons.exception.IllegalArgumentException("没有找到任务执行器");
        }

        taskStartHandler.begin(context); //开始任务

        context.hasTaxInstance().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("不能发起任务,请求参数错误[不存在instance]"));
        context.hasTaxInstanceCategory().orElseThrow(() -> new com.yun9.commons.exception.IllegalArgumentException("不能发起任务,请求参数错误[不存在instanceCategory]"));


        Map<Integer, String> errors = new HashMap<>();
        TaxRouterBuilder taxRouterBuilder = taxRouterBuilder(context.getRequest().getActionSn(), context.getRequest().getTaxOffice(), context.getRequest().getCycleType(), context.getBizMdArea().getSn());
        if (null == taskStartHandler) {
            logger.error("没找到税务路由实现");
            throw new com.yun9.commons.exception.IllegalArgumentException("没找到税务路由实现");
        }


        try {
            TaxRouterRequest taxRouterRequest = taxRouterBuilder.build(context);
            //TaskBO taskBO = taskSendHandler.sendToTask(context, taxRouterRequest);
            taskSendHandler.send(context, taxRouterRequest);
            taskStartHandler.success(context);
        } catch (BizException ex) {
            errors.put(ex.getCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (!errors.isEmpty()) {
                taskStartHandler.exception(context, errors);
            }
        }

        return "当前操作成功";
    }


}
