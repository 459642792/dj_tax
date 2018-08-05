package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstUserService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.md.domain.entity.BizMdInstUser;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxNoticeInstConfig;
import com.yun9.biz.tax.domain.entity.BizTaxNoticeInstRecord;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.DateUtils;
import com.yun9.service.tax.core.TaxNoticeInstFactory;
import com.yun9.service.tax.core.utils.OkHttpUtils;
import com.yun9.sys.SysUserService;
import com.yun9.sys.domain.entity.SysUser;
import com.yun9.sys.exception.SysException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yun9.sys.exception.SysException.Codes.NOT_FOUND_USER;

@Component
public class TaxNoticeInstFactoryImpl implements TaxNoticeInstFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxNoticeInstFactoryImpl.class);

    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    private BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    private BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    private BizTaxNoticeInstConfigService bizTaxNoticeInstConfigService;

    @Autowired
    private BizTaxNoticeInstRecordService bizTaxNoticeInstRecordService;

    @Autowired
    private BizMdInstClientService bizMdInstClientService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    private BizMdCompanyService bizMdCompanyService;

    @Autowired
    private BizMdInstUserService bizMdInstUserService;

    @Override
    public BizTaxNoticeInstRecord noticeByBizTaxInstanceCategoryId(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategory instanceCategory = bizTaxInstanceCategoryService.findById(bizTaxInstanceCategoryId);
        if (null == instanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }

        BizTaxInstance instance = bizTaxInstanceService.findById(instanceCategory.getBizTaxInstanceId());
        if (null == instanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.TaxInstanceNotFound);
        }
        BizMdInstClient instClient = bizMdInstClientService.findById(instance.getMdInstClientId());
        BizTaxNoticeInstConfig noticeInstConfig = bizTaxNoticeInstConfigService.findByMdInstIdAndNoticeType(instClient.getBizMdInstId(), BizTaxNoticeInstConfig.NoticeType.declareResult.name());
        if (null == noticeInstConfig) {
            return null; // 无机构通知配置，不通知
        }
        BizTaxNoticeInstRecord noticeInstRecord = buildRecord(instanceCategory, instance, instClient, noticeInstConfig);
        noticeInstRecord = bizTaxNoticeInstRecordService.create(noticeInstRecord);
        startNotice(noticeInstRecord, noticeInstConfig);
        return noticeInstRecord;
    }

    private void startNotice(BizTaxNoticeInstRecord noticeInstRecord, BizTaxNoticeInstConfig noticeInstConfig) {
        try {
            OkHttpClient okHttpClient = OkHttpUtils.getClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(mediaType, noticeInstRecord.getParams());
            Request request = new Request.Builder()
                    .url(noticeInstConfig.getActionUrl())
                    .post(requestBody)
                    .addHeader("content-type", "application/json")
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();
            response.close();
            noticeInstRecord.setRemark(responseBody);
            if (responseBody.indexOf("\"errcode\":\"1\"") != -1) {
                noticeInstRecord.setState(BizTaxNoticeInstRecord.State.success);
            } else {
                noticeInstRecord.setState(BizTaxNoticeInstRecord.State.exception);
            }
        } catch (Exception ex) {
            logger.error("发送机构通知失败：" + noticeInstRecord.getId(), ex);
            noticeInstRecord.setRemark(ex.getMessage());
            noticeInstRecord.setState(BizTaxNoticeInstRecord.State.exception);
        }
        bizTaxNoticeInstRecordService.create(noticeInstRecord);
    }

    private BizTaxNoticeInstRecord buildRecord(BizTaxInstanceCategory instanceCategory, BizTaxInstance instance, BizMdInstClient instClient, BizTaxNoticeInstConfig noticeInstConfig) {
        BizMdInstUser instUser = bizMdInstUserService.findById(instanceCategory.getCreatedBy());
        if (null == instUser) {
            throw SysException.build(NOT_FOUND_USER);
        }
        SysUser sysUser = sysUserService.findById(instUser.getUserId());
        if (null == sysUser) {
            throw SysException.build(NOT_FOUND_USER);
        }
        BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findById(instance.getMdAccountCycleId());
        if (null == instanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.DateError, "找不到税期：" + instance.getMdAccountCycleId());
        }
        BizTaxMdCategory taxMdCategory = bizTaxMdCategoryService.findById(instanceCategory.getBizTaxMdCategoryId());
        BizMdCompany bizMdCompany = bizMdCompanyService.findById(instClient.getBizMdCompanyId());
        BizTaxNoticeInstRecord noticeInstRecord = new BizTaxNoticeInstRecord();
        noticeInstRecord.setBizTaxInstanceCategoryId(instanceCategory.getId());
        noticeInstRecord.setBizTaxNoticeInstConfigId(noticeInstConfig.getId());
        noticeInstRecord.setMdCompanyd(instance.getMdCompanyId());
        noticeInstRecord.setMdInstId(instClient.getBizMdInstId());
        noticeInstRecord.setNoticeType(BizTaxNoticeInstConfig.NoticeType.declareResult.name());
        noticeInstRecord.setState(BizTaxNoticeInstRecord.State.none);
        noticeInstRecord.setTaxSn(bizMdAccountCycle.getSn());
        JSONObject params = new JSONObject();
        boolean isSuccess = (
                instanceCategory.getState() == BizTaxInstanceCategory.State.deduct ||
                        instanceCategory.getState() == BizTaxInstanceCategory.State.complete
        ) ? true : false;
        boolean isPay = (
                instanceCategory.getState() == BizTaxInstanceCategory.State.complete
        ) ? true : false;
        params.put("instid", instClient.getBizMdInstId());
        params.put("accountcyclesn", bizMdAccountCycle.getSn());
        params.put("taxsn", taxMdCategory.getSn());
        if (instanceCategory.getTaxOfficeConfirm() == BizTaxInstanceCategory.TaxOfficeConfirm.disabled) {
            params.put("state", "notax");
        } else {
            params.put("state", isSuccess ? "success" : "none");
        }
        params.put("taxno", bizMdCompany.getTaxNo());
        params.put("operator", sysUser.getName());
        params.put("sendtype", instanceCategory.getDeclareType().name());
        params.put("senddate", isSuccess ?
                DateUtils.Format.YYYYMMDDHHMM.
                        StringValue(DateUtils.unixTimeToLocalDateTime(instanceCategory.getDeclareDate()),
                                DateUtils.ZH_PATTERN_SECOND) : null);
        params.put("declaresn", instanceCategory.getTaxPaySn());
        params.put("paytaxdate", isPay ? DateUtils.Format.YYYYMMDDHHMM.
                StringValue(DateUtils.unixTimeToLocalDateTime(instanceCategory.getUpdatedAt()),
                        DateUtils.ZH_PATTERN_SECOND)  : null);
        params.put("paytaxstate", isPay ? "success" : "none");
        params.put("paytaxmessage", isPay ? instanceCategory.getProcessMessage() : null);
        params.put("taxOfficePayAmount",instanceCategory.getTaxOfficePayAmount());
        params.put("realPayAmount",instanceCategory.getRealPayAmount());
        params.put("taxPayAmount",instanceCategory.getTaxPayAmount());
        noticeInstRecord.setParams(params.toJSONString());
        return noticeInstRecord;
    }
}
