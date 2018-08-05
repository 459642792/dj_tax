package com.yun9.service.tax.core.v2.handler.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.yun9.biz.task.BizTaskService;
import com.yun9.biz.task.domain.bo.StartTaskBO;
import com.yun9.biz.task.domain.bo.TaskBO;
import com.yun9.biz.tax.BizTaxInstanceSeqService;
import com.yun9.biz.tax.BizTaxTaskPropertyService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceSeq;
import com.yun9.biz.tax.domain.entity.properties.BizTaxTaskProperty;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.webdriver.http.Header;
import com.yun9.framework.webdriver.http.WebDriver;
import com.yun9.service.tax.core.ServiceTaxProperties;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxSendTaskException;
import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import lombok.Data;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

/**
 * Created by werewolf on  2018/5/22.
 */
@Component
public class TaskSendHandlerImpl implements TaskSendHandler {

    public static final Logger logger = LoggerFactory.getLogger(TaskSendHandler.class);
    @Autowired
    BizTaskService bizTaskService;

    @Autowired
    BizTaxTaskPropertyService bizTaxTaskPropertyService;

    @Autowired
    ServiceTaxProperties serviceTaxProperties;
    @Autowired
    BizTaxInstanceSeqService bizTaxInstanceSeqService;

    private static volatile HttpClient httpClient;

    public static HttpClient buildHttpClient() {
        if (null == httpClient) {
            logger.debug("-----------build httpclient--------------");
            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec("standard").setConnectTimeout(50000).setSocketTimeout(50000).build();
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setDefaultMaxPerRoute(20);
            connectionManager.setMaxTotal(100); //todo 改成配置
            CookieStore cookieStore = new BasicCookieStore();
            httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).setConnectionManager(connectionManager).setDefaultCookieStore(cookieStore).build();
        }

        return httpClient;
    }

    @Override
    public void send(OperationContext context, TaxRouterRequest taxRouterRequest) {
        WebDriver webDriver = WebDriver.build(buildHttpClient());
        logger.info("开始查找任务定义信息getActionSn-{},getBizMdArea->{},taxOffice->{}", context.getRequest().getActionSn().toString(),//业务操作
                context.getBizMdArea().getId(), //地区
                context.getRequest().getTaxOffice());
        BizTaxTaskProperty bizTaxTaskProperty = bizTaxTaskPropertyService.findByBizSnAndMdAreaSnAndTaxOffice(
                context.getRequest().getActionSn().toString(),//业务操作
                context.getBizMdArea().getId(), //地区
                context.getRequest().getTaxOffice());//税区
        if (null == bizTaxTaskProperty) {
            logger.error("错误的请求参数未能找到任务【业务操作{},地区{}，税区{}】",
                    context.getRequest().getActionSn(), context.getBizMdArea().getSn(), context.getRequest().getTaxOffice());
            throw ServiceTaxException.build(ServiceTaxException.Codes.task_start_failed_has_not_task_sn);
        }
        logger.info("开始查找任务定义信息-->{}", bizTaxTaskProperty);

        if (StringUtils.isNotEmpty(bizTaxTaskProperty.getLoginType())) {
            if (!bizTaxTaskProperty.getLoginType().contains(taxRouterRequest.getLoginInfo().getLoginType())) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.task_start_send_failed1, "当前任务【" + bizTaxTaskProperty.getBizName() + "】登录方式【" + taxRouterRequest.getLoginInfo().getLoginType() + "】不在系统配置等登录方式中");
            }
        }

        StartTaskBO startTaskBO = new StartTaskBO();
        startTaskBO.setBizKey(context.getBizMdAccountCycle().getSn() + bizTaxTaskProperty.getBizName() + bizTaxTaskProperty.getTaxOffice() + bizTaxTaskProperty.getTaskSn() + "[" + context.getBizMdCompany().getFullName() + "]");
        context.setTaskSn(bizTaxTaskProperty.getTaskSn());
        context.setTaskBizId(String.valueOf(context.getBizMdCompany().getId()));
        if (serviceTaxProperties.isMock()) {
            startTaskBO.setBizId(StringUtils.isNotEmpty(context.getBizMdCompany().getFtId()) ? context.getBizMdCompany().getFtId() : String.valueOf(context.getBizMdCompany().getId()));
            startTaskBO.setInstId(context.getBizMdInst().getFtId());
        } else {
            startTaskBO.setBizId(String.valueOf(context.getBizMdCompany().getId()));
            startTaskBO.setInstId(String.valueOf(context.getBizMdInst().getId()));
        }
        startTaskBO.setMaintainBy("");//todo 公司运维任务
        startTaskBO.setInstName(context.getBizMdInst().getName());
        startTaskBO.setAccountCycleSn(context.getBizMdAccountCycle().getSn());

        startTaskBO.setCallbackUrl(new String(Base64.getEncoder().encode(serviceTaxProperties.getTaskCallBackUrl().getBytes())));
        startTaskBO.setBody(JSON.toJSONString(new HashMap() {{
            put("body", taxRouterRequest);
            put("taskDefineSn", bizTaxTaskProperty.getTaskSn());
        }}, SerializerFeature.WriteNonStringKeyAsString, SerializerFeature.WriteNonStringValueAsString, SerializerFeature.WriteMapNullValue));


        try {
            if (logger.isDebugEnabled()) {
                logger.debug("发送到任务中心请求参数{}", JSON.toJSONString(startTaskBO));
            }
            TaskResponse response = webDriver.defaultPost(serviceTaxProperties.buildTaskUrl(bizTaxTaskProperty.getTaskSn(), context.getRequest().getUserId() + ""),
                    null,
                    new Header().add("Content-Type", "application/json;charset=UTF-8").add("Accept", "application/json"),
                    JSON.toJSONString(startTaskBO)).json(TaskResponse.class);
            if (logger.isDebugEnabled()) {
                logger.debug("发送任务返回参数{}", JSON.toJSONString(response));
            }
            if (200 != response.status) {
                throw new ServiceTaxSendTaskException(response.getMessage(), response.getStatus());
            }
            context.setTaskSeq(response.getBody().getSeq());
            context.setTaskInstanceId(response.getBody().getId());
        } catch (BizException ex) {
            logger.error(ex.getMessage());
            throw new ServiceTaxSendTaskException(ex.getMessage(), ex.getCode());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new ServiceTaxSendTaskException("发送任务失败,任务中心不可用");
        }
    }

    @Data
    public static class TaskResponse {
        private String message;
        private int status;
        private TaskBody body;
    }

    @Data
    public static class TaskBody {
        private String seq;
        private long id;
    }

    //  @Override
    public TaskBO sendToTask(OperationContext context, TaxRouterRequest taxRouterRequest) {

        BizTaxTaskProperty bizTaxTaskProperty = bizTaxTaskPropertyService.findByBizSnAndMdAreaSnAndTaxOffice(
                context.getRequest().getActionSn().toString(),//业务操作
                context.getBizMdArea().getId(), //地区
                context.getRequest().getTaxOffice());//税区
        if (null == bizTaxTaskProperty) {
            logger.error("错误的请求参数未能找到任务【业务操作{},地区{}，税区{}】",
                    context.getRequest().getActionSn(), context.getBizMdArea().getSn(), context.getRequest().getTaxOffice());
            throw ServiceTaxException.build(ServiceTaxException.Codes.task_start_failed_has_not_task_sn);

        }
        StartTaskBO startTaskBO = new StartTaskBO();
        startTaskBO.setBizKey(context.getBizMdAccountCycle().getSn() + bizTaxTaskProperty.getBizName() + bizTaxTaskProperty.getTaxOffice() + bizTaxTaskProperty.getTaskSn() + "[" + context.getBizMdCompany().getFullName() + "]" + taxRouterRequest.getLoginInfo().getLoginType());
        startTaskBO.setBizId(context.getBizMdCompany().getId() + "");
        startTaskBO.setMaintainBy("");//todo 公司运维任务
        startTaskBO.setInstId(String.valueOf(context.getBizMdInst().getId()));
        startTaskBO.setInstName(context.getBizMdInst().getName());
        startTaskBO.setAccountCycleSn(context.getBizMdAccountCycle().getSn());

        startTaskBO.setBody(JSON.toJSONString(new HashMap() {{
            put("body", taxRouterRequest);
            put("taskDefineSn", bizTaxTaskProperty.getTaskSn());
        }}, SerializerFeature.WriteNonStringKeyAsString, SerializerFeature.WriteNonStringValueAsString, SerializerFeature.WriteMapNullValue));

        startTaskBO.setCallbackUrl(new String(Base64.getEncoder().encode(serviceTaxProperties.getTaskCallBackUrl().getBytes())));

        if (logger.isDebugEnabled()) {
            logger.debug("开始发起{}任务", JSON.toJSONString(startTaskBO));
        }

        TaskBO taskBO = bizTaskService.start(serviceTaxProperties.getTaskEnv(), bizTaxTaskProperty.getTaskSn(), String.valueOf(context.getRequest().getUserId()), startTaskBO);

        if (null == taskBO) {
            logger.error("发起任务失败");
            throw ServiceTaxException.build(ServiceTaxException.Codes.task_start_send_failed);
        }

        return taskBO;
    }

    @Override
    public void processTask(OperationContext context, TaxRouterRequest taxRouterRequest) {
        TaskBO taskBO = this.sendToTask(context, taxRouterRequest);
        BizTaxInstanceSeq bizTaxInstanceSeq = new BizTaxInstanceSeq();
        bizTaxInstanceSeq.setState(BizTaxInstanceSeq.State.process);
        bizTaxInstanceSeq.setSeq(taskBO.getBizTaskInstance().getSeq());
        bizTaxInstanceSeq.setBizTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId());
        bizTaxInstanceSeq.setBizTaxInstanceId(context.getBizTaxInstance().getId());
        bizTaxInstanceSeqService.save(bizTaxInstanceSeq);

    }
}
