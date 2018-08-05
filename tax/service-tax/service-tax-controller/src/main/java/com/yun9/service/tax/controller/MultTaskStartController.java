package com.yun9.service.tax.controller;

import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.ServiceTaxThreadPoolContainer;
import com.yun9.service.tax.core.dto.BizMultiTaskSyncClientDTO;
import com.yun9.service.tax.core.v2.MultOperationRequest;
import com.yun9.service.tax.core.v2.OperationRequest;
import com.yun9.service.tax.core.v2.TaxStartFactory;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.domain.dto.MultiScreenShotTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量任务接口
 */
@Controller
@RequestMapping("multtask/start")
public class MultTaskStartController {

    private final static Logger logger = LoggerFactory.getLogger(MultTaskStartController.class);

    private static final String SUM_KEY = "SUM";

    @Autowired
    private TaxStartFactory taxStartFactory;

    @Autowired
    private BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    private BizMdInstClientService bizMdInstClientService;

    @Autowired
    private ServiceTaxThreadPoolContainer serviceTaxThreadPoolContainer;

    @Autowired
    private BizTaxCompanyTaxService bizTaxCompanyTaxService;

    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @PostMapping
    @ResponseBody
    public Object start(@RequestBody @Valid MultOperationRequest request,
                        @User UserDetail userDetail) {
        request.setUserId(userDetail.getId());
        return taxStartFactory.handler(request);
    }

    @PostMapping("/screenshots")
    @ResponseBody
    public Object screenShot(@RequestBody @Valid MultiScreenShotTaskDTO request,
                        @User UserDetail userDetail) {
        request.setUserId(userDetail.getId());
        final Map<String,Integer> record = new HashMap<String,Integer>(){{
            this.put(SUM_KEY,0);
        }};
        startScreenShotTasks(record,request,0,200);
        return record;
    }

    private void startScreenShotTasks(Map<String,Integer> record,MultiScreenShotTaskDTO request,int page,int limit) {
        List<BizTaxInstanceCategory> instanceCategories =
                bizTaxInstanceCategoryService.
                        findAllByTaxOfficeAndDeclareCheckState(request.getTaxOffice(),
                                request.getDeclareCheckState(),
                                request.getMdAccountCycleId(),page,limit);
        if (CollectionUtils.isEmpty(instanceCategories) ) {
            return ;
        }
        if (request.getLimit() > 0) {
            if (record.get(SUM_KEY) > request.getLimit()) {
                return;
            }
        }
        instanceCategories.stream().forEach((item) -> {
            if (request.getLimit() > 0) {
                if (record.get(SUM_KEY) > request.getLimit()) {
                    return;
                }
            }
            record.put(SUM_KEY,record.get(SUM_KEY)+1);
            serviceTaxThreadPoolContainer.getMultTaskThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    OperationRequest actionRequest = new OperationRequest();
                    try {
                        actionRequest.setUserId(request.getUserId());
                        actionRequest.setTaxInstanceCategoryId(item.getId());
                        actionRequest.setParams(new HashMap<String,Object>(){{
                            this.put("synType","all");
                        }});
                        actionRequest.setActionSn(ActionSn.syn_screenshot);
                        taxStartFactory.handler(actionRequest);
                    } catch (Exception ex) {
                        logger.error("发起批量截图失败", ex);
                        logger.error(JSONObject.toJSONString(actionRequest));
                    }
                }
            });
        });
        startScreenShotTasks(record,request,(page+1),limit);
    }

    @PostMapping("/syncallclient")
    @ResponseBody
    public Object syncClient(@RequestBody BizMultiTaskSyncClientDTO syncClientDTO,
                             @User UserDetail userDetail) {
        syncClientDTO.setUserId(userDetail.getId());
        List<BizMdCompanyAccount> bizMdCompanyAccounts = bizMdCompanyAccountService.findByBizMdAccountIdAndState(syncClientDTO.getBizMdAccountId(), BizMdCompanyAccount.State.activate.name());
        if (CollectionUtils.isEmpty(bizMdCompanyAccounts)) {
            return null;
        }
        final Map<String,Integer> record = new HashMap<String,Integer>(){{
            this.put(SUM_KEY,0);
        }};
        bizMdCompanyAccounts.stream().forEach((bizMdCompanyAccount) -> {
            if (null != syncClientDTO.getLimit() && record.get(SUM_KEY) >= syncClientDTO.getLimit()) {
                return;
            }
            if (syncClientDTO.getActionSn() == ActionSn.syn_company_tax) {
                BizTaxCompanyTax bizMdCompanyTax = bizTaxCompanyTaxService.findByCompanyId(bizMdCompanyAccount.getCompanyId());
                if (bizMdCompanyTax != null && (bizMdCompanyTax.getSyncState() == BizTaxCompanyTax.SyncState.sync ||
                bizMdCompanyTax.getProcessState() != BizTaxCompanyTax.ProcessState.none)) {
                    return;
                }
            }
            List<BizMdInstClient> bizMdInstClients = new ArrayList<>();
            if (null != syncClientDTO.getInstId()) {
                BizMdInstClient instClient = bizMdInstClientService.findByCompanyIdAndInstId(bizMdCompanyAccount.getCompanyId(),syncClientDTO.getInstId());
                if (null != instClient) {
                    bizMdInstClients.add(instClient);
                }
            } else {
                bizMdInstClients = bizMdInstClientService.findByCompanyId(bizMdCompanyAccount.getCompanyId());
            }
            if (CollectionUtils.isEmpty(bizMdInstClients)) {
                return;
            }
            bizMdInstClients.stream().forEach((instClient) -> {
                if (null != syncClientDTO.getExcludeInsts() && syncClientDTO.getExcludeInsts().contains(instClient.getBizMdInstId())) {
                    return;
                }
                record.put(SUM_KEY,record.get(SUM_KEY)+1);
                serviceTaxThreadPoolContainer.getMultTaskThreadPool().execute(() -> {
                    OperationRequest actionRequest = new OperationRequest();
                    try {
                        BeanUtils.copyProperties(syncClientDTO, actionRequest);
                        actionRequest.setInstId(instClient.getBizMdInstId());
                        actionRequest.setCompanyId(instClient.getBizMdCompanyId());
                        actionRequest.setActionSn(syncClientDTO.getActionSn());
                        taxStartFactory.handler(actionRequest);
                    } catch (Exception ex) {
                        logger.error("发起批量同步客户失败", ex);
                        logger.error(JSONObject.toJSONString(actionRequest));
                    }
                });
            });

        });
        return "即将发起任务数：" + record.get(SUM_KEY);
    }
}
