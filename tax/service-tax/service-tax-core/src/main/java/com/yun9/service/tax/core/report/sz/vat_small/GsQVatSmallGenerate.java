package com.yun9.service.tax.core.report.sz.vat_small;

import com.yun9.biz.bill.BizBillInvoiceCalculateService;
import com.yun9.biz.bill.BizBillInvoiceService;
import com.yun9.biz.bill.domain.bo.CalculateAgentDto;
import com.yun9.biz.bill.domain.bo.CalculateNoBillDto;
import com.yun9.biz.bill.domain.bo.CalculateOutputDto;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.tax.BizTaxInstanceCategoryDataService;
import com.yun9.biz.tax.BizTaxInstanceCategoryVatSmallService;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryData;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryVatSmall;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.report.IReportGenerate;
import com.yun9.service.tax.core.report.ReportSnMapping;
import com.yun9.service.tax.core.report.sz.vat_small.report.Report001;
import com.yun9.service.tax.core.report.sz.vat_small.report.Report002;
import com.yun9.service.tax.core.report.sz.vat_small.report.Report003;
import com.yun9.service.tax.core.task.helper.JsonMap;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by werewolf on  2018/6/1.
 */
@ReportSnMapping(sns = "shenzhen_gs_q_small_vat")
public class GsQVatSmallGenerate implements IReportGenerate {

    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(GsQVatSmallGenerate.class);
    @Autowired
    private BizTaxInstanceCategoryDataService bizTaxInstanceCategoryDataService;

    @Autowired
    private BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;

    @Autowired
    private BizBillInvoiceCalculateService bizBillInvoiceCalculateService;


    @Autowired
    private BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    private BizBillInvoiceService bizBillInvoiceService;

    @Override
    public Map<String, List<ReportDataDTO>> generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body) {

        long s1 = System.currentTimeMillis();
        //已经申报，保存历史数据
        if (bizTaxInstanceCategory.getDeclareType() == DeclareType.taxOffice) {
            Map<String, List<ReportDataDTO>> result = new HashMap<>();
            List<JsonMap> reports = body.getArray("reportData");
            if (CollectionUtils.isNotEmpty(reports)) {
                for (JsonMap jsonMap : reports) {
                    if (jsonMap.getString("sn").equals("001")) {
                        Map<String, Object> report001 = jsonMap.getObject("data");
                        result.put("shenzhen_gs_q_small_vat_xgmzzssbb", ReportDataDTO.bulidReportDataDTO(jsonMap.getObject("data")));
                        bizTaxInstanceCategoryVatSmallService.generateReportAfterProcess(report001, bizTaxInstanceCategory.getId());
                    } else if (jsonMap.getString("sn").equals("002")) {
                        result.put("shenzhen_gs_q_small_vat_zzsnssbbflzl", ReportDataDTO.bulidReportDataDTO(jsonMap.getObject("data")));
                    } else if (jsonMap.getString("sn").equals("003")) {
                        result.put("shenzhen_gs_q_small_vat_zzsjmssbmxb", ReportDataDTO.bulidReportDataDTO(jsonMap.getObject("data")));
                    }
                }
                return result;
            } else {
                return null;
            }
        }


        if (!(bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.start) || bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.send))) {

            logger.debug("生成vat报表出现错误,当前税种状态为id-{}_{}", bizTaxInstanceCategory.getId(), bizTaxInstanceCategory.getState());
            //状态不是start
            return null;
        }
        //历史申报数据
        BizTaxInstanceCategoryData bizTaxInstanceCategoryDataHistory = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.history, bizTaxInstanceCategory.getId());
        BizTaxInstanceCategoryData bizTaxInstanceCategoryDataCurrent = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.current, bizTaxInstanceCategory.getId());
        if ((null == bizTaxInstanceCategoryDataHistory || "[]".equals(bizTaxInstanceCategoryDataHistory.getJson())) || (null == bizTaxInstanceCategoryDataCurrent || "[]".equals(bizTaxInstanceCategoryDataCurrent.getJson()))) {
            logger.error("没有下载到增值税历史数据.{}", bizTaxInstanceCategory.getProcessMessage());
            ServiceTaxException.throwException(ServiceTaxException.Codes.ReportCreateException, "无法找到税种下载的历史数据");
            // return null;
        }
        final Map<String, String> history = bizTaxInstanceCategoryDataHistory.toMap();
        //当前数据
        final Map<String, String> current = bizTaxInstanceCategoryDataCurrent.toMap();

        Report001.CalculationValue calculationValue = new Report001.CalculationValue();

        if (new BigDecimal(current.get("YSHWYNZSL")).compareTo(new BigDecimal("0.00")) != 0 && (current.get("SZLBDM").equals("01") || current.get("SZLBDM").equals("03"))) {
            calculationValue.labourstate = true;
        }
        if (new BigDecimal(current.get("YSFWYNZSL")).compareTo(new BigDecimal("0.00")) != 0 && (current.get("SZLBDM").equals("01") || current.get("SZLBDM").equals("03"))) {
            calculationValue.servicestate = true;
        }
        if (current.get("GTHBZ").equals("1")) {
            calculationValue.personalstate = true;
        }
        //获取开票金额
        if (StringUtils.isNotEmpty(current.get("DKFPJE"))) {
            calculationValue.agentTotalAmount = new BigDecimal(current.get("DKFPJE"));
        }
        final BizTaxInstance bizTaxInstance = bizTaxInstanceCategory.getBizTaxInstance();
        List<Long> _accountCycleIds = bizMdAccountCycleService.findContainsMonth(bizTaxInstance.getMdAccountCycleId()).stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());
        final long[] accountCycleIds = _accountCycleIds.stream().mapToLong(Long::longValue).toArray();
        //计算代开金额
        CalculateAgentDto calculateAgentDto = bizBillInvoiceCalculateService.calculateAgent(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateAgentDto) {
            calculationValue.sstAgentAmount = calculateAgentDto.getSstAgentAmount();
            calculationValue.sptAgentAmount = calculateAgentDto.getSptAgentAmount();
            calculationValue.ssfAgentAmount = calculateAgentDto.getSsfAgentAmount();
            calculationValue.spfAgentAmount = calculateAgentDto.getSpfAgentAmount();
            calculationValue.lstAgentAmount = calculateAgentDto.getLstAgentAmount();
            calculationValue.lptAgentAmount = calculateAgentDto.getLptAgentAmount();
            calculationValue.agentAmount = calculateAgentDto.getAgentAmount();
        }

        //   logger.debug("公司{},会计期间{}代开发票信息{}", bizTaxInstance.getMdCompanyId(), accountCycleIds, JSON.toJSONString(calculateAgentDto));
        //计算不开票
        CalculateNoBillDto calculateNoBillDto = bizBillInvoiceCalculateService.calculateNoBill(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateNoBillDto) {
            calculationValue.lnNobillAmount = calculateNoBillDto.getLnNobillAmount();
            calculationValue.sntNobillAmount = calculateNoBillDto.getSntNobillAmount();
            calculationValue.snfNobillAmount = calculateNoBillDto.getSnfNobillAmount();
        }
        //  logger.debug("公司{},会计期间{}无票信息{}", bizTaxInstance.getMdCompanyId(), accountCycleIds, JSON.toJSONString(calculateNoBillDto));
        //计算自开发票
        CalculateOutputDto calculateOutputDto = bizBillInvoiceCalculateService.calculateOutput(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateOutputDto) {
            calculationValue.lnOutputAmount = calculateOutputDto.getLnOutputAmount();
            calculationValue.lstAgentAmount = calculateOutputDto.getLstOutputAmount().add(calculateAgentDto.getLstAgentAmount());
            calculationValue.ltOutputAmount = calculateOutputDto.getLtOutputAmount();
            calculationValue.snOutputAmount = calculateOutputDto.getSnOutputAmount();
            calculationValue.stOutputAmount = calculateOutputDto.getStOutputAmount();
            calculationValue.sfOutputAmount = calculateOutputDto.getSfOutputAmount();
            calculationValue.sstAgentAmount = calculateOutputDto.getSstOutputAmount().add(calculateAgentDto.getSstAgentAmount());
            calculationValue.ssfAgentAmount = calculateOutputDto.getSsfOutputAmount().add(calculateAgentDto.getSsfAgentAmount());
        }
//        else {
//            calculationValue.sstAgentAmount = calculateAgentDto.getSstAgentAmount();
//            calculationValue.ssfAgentAmount = calculateAgentDto.getSsfAgentAmount();
//        }

        //    logger.debug("公司{},会计期间{}自开发票信息{}", bizTaxInstance.getMdCompanyId(), accountCycleIds, JSON.toJSONString(calculateOutputDto));


        //查预缴税款
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = bizTaxInstanceCategoryVatSmallService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (bizTaxInstanceCategoryVatSmall.getPrepayTaxSource() == BizTaxInstanceCategoryVatSmall.PrepayTaxSource.taxoffice) {
            calculationValue.ltPrepaidAmount = bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoDeclareamount();
            calculationValue.stPrepaidAmount = bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceDeclareamount();
            calculationValue.ltPrepaidAmounttotal = bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoTotalamount();
            calculationValue.stPrepaidAmounttotal = bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceTotalamount();
        } else if (bizTaxInstanceCategoryVatSmall.getPrepayTaxSource() == BizTaxInstanceCategoryVatSmall.PrepayTaxSource.agent) {
            HashMap<String, BigDecimal> amounts = bizBillInvoiceService.countAmountByCompanyId(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(), _accountCycleIds);
            calculationValue.ltPrepaidAmount = amounts.get("cargo");// bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoDeclareamount();
            calculationValue.stPrepaidAmount = amounts.get("service");//bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceDeclareamount();
            calculationValue.ltPrepaidAmounttotal = bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoTotalamount();
            calculationValue.stPrepaidAmounttotal = bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceTotalamount();

        } else {
            ServiceTaxException.throwException(ServiceTaxException.Codes.ReportCreateException, "无法确定预缴类型");
        }


        logger.debug("初始化report001数据{}", System.currentTimeMillis() - s1);

        Map<String, Object> report001 = new Report001().generate(calculationValue, history).toMap();

        //==============生成报表后更新增值税信息====================
        bizTaxInstanceCategoryVatSmallService.generateReportAfterProcess(report001, bizTaxInstanceCategory.getId());

        return new HashMap() {{
            put("shenzhen_gs_q_small_vat_xgmzzssbb", ReportDataDTO.bulidReportDataDTO(report001));
            put("shenzhen_gs_q_small_vat_zzsnssbbflzl", ReportDataDTO.bulidReportDataDTO(new Report002().generate().toMap()));
            String FB4JZEQCYE = current.get("FB4JZEQCYE");
            if (StringUtils.isEmpty(FB4JZEQCYE)) {
                FB4JZEQCYE = "0.00";
            }
            put("shenzhen_gs_q_small_vat_zzsjmssbmxb", ReportDataDTO.bulidReportDataDTO(new Report003().generate(new BigDecimal(current.get("FB4JZEQCYE"))).toMap()));
        }};
    }

    @Override
    public boolean isResetCreate() {
        return true;
    }
}
