package com.yun9.service.tax.core.task.callback.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceSeq;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.biz.tax.ops.BizTaxTaskCallService;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.ServiceTaxThreadPoolContainer;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import com.yun9.service.tax.core.task.callback.TaskCallBackHandler;
import com.yun9.service.tax.core.task.callback.TaskCallBackResponse;
import com.yun9.service.tax.core.task.callback.TaskCallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.CallbackProcessFailed;

/**
 * 任务回调处理工厂
 */

@Component
public class TaskCallbackFactoryImpl implements TaskCallbackFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaskCallbackFactoryImpl.class);


    @Autowired
    private List<TaskCallBackHandler> taskCallBackHandlers;

    private static Map<String, TaskCallBackHandler> callBackSnHandlers = new ConcurrentHashMap<>();

    @Autowired
    private BizTaxInstanceSeqService bizTaxInstanceSeqService;

    @Autowired
    private BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    private BizTaxTaskCallService bizTaxTaskCallService;

    @Autowired
    private BizTaxCompanyTaxService bizTaxCompanyTaxService;

    @Autowired
    private BizTaxInstanceCategoryOriginalDataService bizTaxInstanceCategoryOriginalDataService;
    @Autowired
    ServiceTaxThreadPoolContainer serviceTaxThreadPoolContainer;
    @Autowired
    BizTaxMdMsgCodeService bizTaxMdMsgCodeService;

    @PostConstruct
    public void init() {
        taskCallBackHandlers.forEach(v -> {
            TaskSnMapping taskSn = v.getClass().getAnnotation(TaskSnMapping.class);
            Arrays.stream(taskSn.sns()).forEach(sn -> callBackSnHandlers.put(sn, v));
        });
        logger.info("初始化SN回调处理器---");
        callBackSnHandlers.forEach((k, v) -> {
            logger.info("callback:{}_{}", k, v.getClass().getName());
        });
    }

    private List<String> specialSn = new ArrayList() {{
        add("SZ0014");
    }};

    @Override
    public void callback(TaskCallBackResponse callBackResponse) {
        if (callBackResponse == null) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskCallBackResponseNotNull);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("回调参数{}", JSON.toJSONString(callBackResponse));
        }

        logger.debug("开始处理任务回调,Seq:{}", callBackResponse.getSeq());
        if (StringUtils.isEmpty(callBackResponse.getSeq())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskCallSeqNotNull);
        }

        //检查task-sn
        if (StringUtils.isEmpty(callBackResponse.getSn())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskCallTaskSnNotNull);
        }

        //查找任务处理器
        logger.debug("开始查找任务类型：{},的任务回调处理器", callBackResponse.getSn());
        TaskCallBackHandler taskCallBackHandler = callBackSnHandlers.get(callBackResponse.getSn());

        if (taskCallBackHandler == null) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskCallBackHandlerNotFound);
        }

        //构建任务处理上下文
        TaskCallBackContext taskCallBackContext = new TaskCallBackContext();
        taskCallBackContext.setBody(callBackResponse.getBody());
        taskCallBackContext.setCreateBy("task-center"); //todo
        taskCallBackContext.setTaskCallBackResponse(callBackResponse);

        //特殊sn 处理
        if (specialSn.contains(callBackResponse.getSn())) {
            this.log(taskCallBackContext);
            taskCallBackHandler.process(taskCallBackContext);
            return;
        }


        //查找处理中的Seq
        BizTaxInstanceSeq bizTaxInstanceSeq = bizTaxInstanceSeqService.findBySeqAndState(callBackResponse.getSeq(), BizTaxInstanceSeq.State.process);

        //如果找不到seq,直接返回，不抛出异常
        if (bizTaxInstanceSeq == null) {
            logger.warn("回调任务seq：{}，无法找到！跳过本次处理!", callBackResponse.getSeq());
            return;
        }
        taskCallBackContext.setBizTaxInstanceSeq(bizTaxInstanceSeq);
        //查找对应的任务实例
        BizTaxInstance bizTaxInstance = bizTaxInstanceService.findById(bizTaxInstanceSeq.getBizTaxInstanceId());
        if (bizTaxInstance == null) {
            logger.warn("回调任务seq：{}，无法找到任务处理实例：{}，！跳过本次处理!", callBackResponse.getSeq(), bizTaxInstanceSeq.getBizTaxInstanceId());
            return;
        }
        taskCallBackContext.setBizTaxInstance(bizTaxInstance);

        //执行处理器
        try {

            if (200 == taskCallBackContext.getTaskCallBackResponse().getCode()) {
                this.log(taskCallBackContext);
                taskCallBackHandler.process(taskCallBackContext);
            } else {
                bizTaxTaskCallService.exception(bizTaxInstanceSeq.getSeq(), callBackResponse.getCode(), callBackResponse.getMessage()); //回调
            }
            logger.debug("---------任务处理器执行完成-----------{}", bizTaxInstanceSeq.getTaskSn());
        } catch (Exception e) {
            logger.error(String.format("回调执行错误-%s,",bizTaxInstanceSeq.getTaskSn()), e);
            throw ServiceTaxException.build(CallbackProcessFailed, e.getMessage());
        }
    }

    private void log(final TaskCallBackContext context) {
        CompletableFuture.runAsync(() -> {
            logger.debug("保存原始数据.....{}", context.getBizTaxInstanceSeq());
            if (null != context) {
                if (context.getBizTaxInstanceSeq()!=null){
                    bizTaxInstanceCategoryOriginalDataService.dataLog(
                            context.getBizTaxInstanceSeq().getBizTaxInstanceId(),
                            context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId(),
                            context.getBizTaxInstanceSeq().getTaskSn(),
                            context.getBizTaxInstanceSeq().getSeq(),
                            JSON.toJSONString(context.getTaskCallBackResponse())
                    );
                }else {
                    bizTaxInstanceCategoryOriginalDataService.dataLog(
                            0,
                            0L,
                            "SZ0014",
                            context.getTaskCallBackResponse().getSeq(),
                            JSON.toJSONString(context.getTaskCallBackResponse())
                    );
                }

            }
        }, serviceTaxThreadPoolContainer.getSingleThreadPool());
    }
}


