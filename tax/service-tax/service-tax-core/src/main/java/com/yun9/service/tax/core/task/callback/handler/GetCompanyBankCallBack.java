package com.yun9.service.tax.core.task.callback.handler;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.report.domain.entity.BizReportInstance;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.dto.tax.DeclareTaxCategory;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyBank;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceSeq;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.biz.tax.ops.BizTaxSingleStartService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.JsonUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import com.yun9.service.tax.core.task.helper.JsonMap;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.CallbackProcessFailed;


@TaskSnMapping(sns = {"SZ0009"})
public class GetCompanyBankCallBack extends AbstractCallbackHandlerMapping {
    private static final Logger logger = LoggerFactory.getLogger(GetCompanyBankCallBack.class);

    @Autowired
    private BizTaxCompanyBankService bizTaxCompanyBankService;
    @Autowired
    BizTaxInstanceSeqService bizTaxInstanceSeqService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        JsonMap body = JSON.parseObject(context.getBody(), JsonMap.class);
        final BizTaxInstance bizTaxInstance = context.getBizTaxInstance();
        BizTaxInstanceSeq bizTaxInstanceSeq = context.getBizTaxInstanceSeq();
        bizTaxInstanceSeq.setMessage("回调成功");
        bizTaxInstanceSeq.setCode(200);
        bizTaxInstanceSeq.setState(BizTaxInstanceSeq.State.success);
        bizTaxInstanceSeqService.save(bizTaxInstanceSeq);
        //银行信息的回写
        this.resolveAndProcessBankInfo(bizTaxInstance, body);

        return context;

    }

    //下载银行
    protected void resolveAndProcessBankInfo(BizTaxInstance bizTaxInstance, JsonMap body) {
        List<BizTaxCompanyBank> bizTaxCompanyBanks = this.bankInfo(bizTaxInstance, body);
        if (null != bizTaxCompanyBanks && bizTaxCompanyBanks.size() > 0) {
            //保存银行信息
            bizTaxCompanyBankService.create(bizTaxCompanyBanks);
        }
    }


    private List<BizTaxCompanyBank> bankInfo(BizTaxInstance bizTaxInstance, JsonMap body) {
        List<JsonMap> bankInfoList = body.getArray("bankInfoList");
        if (null != bankInfoList && bankInfoList.size() > 0) {
            List<BizTaxCompanyBank> bizTaxCompanyBanks = new ArrayList<>();
            int firstTax = bankInfoList.size() == 1 ? 1 :0;
            for (JsonMap map : bankInfoList) {
                TaxOffice taxOffice = TaxOffice.ds;
                BizTaxCompanyBank bizTaxCompanyBank = bizTaxCompanyBankService.findByBizMdCompanyIdAndAccountAndTaxOffice(bizTaxInstance.getMdCompanyId(), map.getString("jkzh"), taxOffice);
                if (null == bizTaxCompanyBank) {
                    bizTaxCompanyBank = new BizTaxCompanyBank();
                }
                bizTaxCompanyBank.setTaxOffice(taxOffice);
                bizTaxCompanyBank.setBizMdCompanyId(bizTaxInstance.getMdCompanyId());
                bizTaxCompanyBank.setAccount(map.getString("jkzh"));
                bizTaxCompanyBank.setBankBranchName(map.getString("yhyywdmc"));
                bizTaxCompanyBank.setBankBranchSn(map.getString("yhyywdDm"));
                bizTaxCompanyBank.setBankName(map.getString("yhhbmc"));
                bizTaxCompanyBank.setBankSn(map.getString("yhhDm"));
                bizTaxCompanyBank.setBindSn(map.getString("sfxyh"));
                bizTaxCompanyBank.setDisplayAccount(map.getString("displayYhzh"));
                bizTaxCompanyBank.setTaxOfficeSn(map.getString("skssswjgDm"));
                bizTaxCompanyBank.setIsDrawback(0);
                bizTaxCompanyBank.setIsFirstTax(firstTax);

                bizTaxCompanyBanks.add(bizTaxCompanyBank);
            }
            return bizTaxCompanyBanks;
        }
        return null;
    }


}
