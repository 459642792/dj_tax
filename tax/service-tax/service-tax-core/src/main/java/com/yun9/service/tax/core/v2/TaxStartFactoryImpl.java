package com.yun9.service.tax.core.v2;

import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstService;
import com.yun9.biz.md.domain.entity.BizMdInst;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.ServiceTaxThreadPoolContainer;
import com.yun9.service.tax.core.dto.ExcelDTO;
import com.yun9.service.tax.core.v2.ops.SendQBitAOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaxStartFactoryImpl implements TaxStartFactory, ApplicationContextAware {

    private static final String SUM_KEY = "SUM";

    private final static Logger logger = LoggerFactory.getLogger(TaxStartFactoryImpl.class);

    private static ApplicationContext applicationContext;

    @Autowired
    private BizMdInstClientService bizMdInstClientService;

    @Autowired
    private BizMdInstService bizMdInstService;

    @Autowired
    private ServiceTaxThreadPoolContainer serviceTaxThreadPoolContainer;

    @Override
    public Object handler(OperationRequest actionRequest) {
        ITargetExecutor startHandler = applicationContext.getBean("target_" + actionRequest.getActionSn().getTarget().toString().toLowerCase(), ITargetExecutor.class);
        return startHandler.execute(new OperationContext(actionRequest));
    }

    @Override
    public Object handler(MultOperationRequest multOperationRequest) {
        final Map<String,Integer> record = new HashMap<String,Integer>(){{
            this.put(SUM_KEY,0);
        }};
        // 如果指定了taxInstanceCategoryIds
        if (CollectionUtils.isNotEmpty(multOperationRequest.getTaxInstanceCategoryIds())) {
            multOperationRequest.getTaxInstanceCategoryIds().stream().forEach((bizInstanceCategoryId) -> {
                record.put(SUM_KEY,record.get(SUM_KEY)+1);
                serviceTaxThreadPoolContainer.getMultTaskThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        OperationRequest actionRequest = new OperationRequest();
                        try {
                            actionRequest.setParams(multOperationRequest.getParams());
                            actionRequest.setActionSn(multOperationRequest.getActionSn());
                            actionRequest.setTaxInstanceCategoryId(bizInstanceCategoryId);
                            actionRequest.setUserId(multOperationRequest.getUserId());
                            handler(actionRequest);
                        } catch (Exception ex) {
                            logger.error("发起批量任务失败", ex);
                            logger.error(JSONObject.toJSONString(actionRequest));
                        }
                    }
                });
            });
            return record;
        }
        return record;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TaxStartFactoryImpl.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
