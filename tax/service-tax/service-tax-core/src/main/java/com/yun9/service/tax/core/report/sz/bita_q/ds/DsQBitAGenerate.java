package com.yun9.service.tax.core.report.sz.bita_q.ds;

import com.yun9.biz.bill.BizBillInvoiceCalculateService;
import com.yun9.biz.bill.domain.bo.CalculateAgentDto;
import com.yun9.biz.bill.domain.bo.CalculateNoBillDto;
import com.yun9.biz.bill.domain.bo.CalculateOutputDto;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.tax.BizTaxInstanceCategoryBitService;
import com.yun9.biz.tax.BizTaxInstanceCategoryDataService;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryBit;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryData;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.report.IReportGenerate;
import com.yun9.service.tax.core.report.ReportSnMapping;
import com.yun9.service.tax.core.report.sz.bita_q.ds.report.*;
import com.yun9.service.tax.core.task.helper.JsonMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportSnMapping(sns = "shenzhen_ds_q_bit_a")
public class DsQBitAGenerate implements IReportGenerate {

    @Autowired
    private BizTaxInstanceCategoryDataService bizTaxInstanceCategoryDataService;

    @Autowired
    private BizTaxInstanceCategoryBitService bizTaxInstanceCategoryBitService;

    @Autowired
    private BizBillInvoiceCalculateService bizBillInvoiceCalculateService;

    @Autowired
    private BizMdAccountCycleService bizMdAccountCycleService;

    @Override
    public Map<String, List<ReportDataDTO>> generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body) {

        BizTaxInstanceCategoryData historyData = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.history, bizTaxInstanceCategory.getId());

        if (null == historyData) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.report_history_data_not_found);
        }

        //历史申报数据
        final Map<String, String> history = historyData.toMap();

        BizTaxInstanceCategoryData currentData = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.current, bizTaxInstanceCategory.getId());

        if (null == currentData) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.report_current_data_not_found);
        }

        //当前数据
        final Map<String, String> currents = currentData.toMap();
        Report001.Report001CalculationValue calculationValue = new Report001.Report001CalculationValue();

        BizTaxInstance bizTaxInstance = bizTaxInstanceCategory.getBizTaxInstance();
        final long[] accountCycleIds = bizMdAccountCycleService.findContainsMonth(bizTaxInstance.getMdAccountCycleId()).stream().map(BizMdAccountCycle::getId).mapToLong(Long::longValue).toArray();

        //计算代开金额
        CalculateAgentDto calculateAgentDto = bizBillInvoiceCalculateService.calculateAgent(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateAgentDto) {
            calculationValue.agentAmount = calculateAgentDto.getLptAgentAmount()
                    .add(calculateAgentDto.getLstAgentAmount())
                    .add(calculateAgentDto.getSpfAgentAmount())
                    .add(calculateAgentDto.getSptAgentAmount())
                    .add(calculateAgentDto.getSsfAgentAmount())
                    .add(calculateAgentDto.getSstAgentAmount());
        }

        //计算不开票
        CalculateNoBillDto calculateNoBillDto = bizBillInvoiceCalculateService.calculateNoBill(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateNoBillDto) {
            calculationValue.nobillAmount = calculateNoBillDto.getLnNobillAmount()
                    .add(calculateNoBillDto.getSntNobillAmount())
                    .add(calculateNoBillDto.getSnfNobillAmount());
        }

        //计算自开发票
        CalculateOutputDto calculateOutputDto = bizBillInvoiceCalculateService.calculateOutput(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateOutputDto) {
            calculationValue.outputAmount = calculateOutputDto.getLnOutputAmount()
                    .add(calculateOutputDto.getLstOutputAmount().add(calculateAgentDto.getLstAgentAmount()))
                    .add(calculateOutputDto.getLtOutputAmount())
                    .add(calculateOutputDto.getSnOutputAmount())
                    .add(calculateOutputDto.getStOutputAmount())
                    .add(calculateOutputDto.getSfOutputAmount())
                    .add(calculateOutputDto.getSstOutputAmount().add(calculateAgentDto.getSstAgentAmount()))
                    .add(calculateAgentDto.getSsfAgentAmount().add(calculateAgentDto.getSsfAgentAmount()));
        }
        //查预缴税款
        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = bizTaxInstanceCategoryBitService.findByTaxInstanceId(bizTaxInstanceCategory.getBizTaxInstanceId());
        //营业收入
        calculationValue.incomeProfitAmount = bizTaxInstanceCategoryBit.getSaleAmount();
        //营业成本
        calculationValue.costProfitAmount = bizTaxInstanceCategoryBit.getBuyAmount();
        //利润总额
        calculationValue.profitProfitAmount = bizTaxInstanceCategoryBit.getProfitAmount();

        Map<String, Object> report001 = new Report001().generate(calculationValue, history, currents).toMap();
        bizTaxInstanceCategoryBitService.afterGeneratedReport(report001, bizTaxInstanceCategory.getId());

        return new HashMap() {{
            put("shenzhen_gs_q_bit_a_qysdsjdyjnssbb", ReportDataDTO.bulidReportDataDTO(report001));
            put("shenzhen_gs_q_bit_a_bzssrhsjljmynsssemxb", ReportDataDTO.bulidReportDataDTO(new Report003().generate().toMap()));
            put("shenzhen_gs_q_bit_a_gdzcjszjmxb", ReportDataDTO.bulidReportDataDTO(new Report004().generate(history).toMap()));
            put("shenzhen_gs_q_bit_a_jmsdsemxb", ReportDataDTO.bulidReportDataDTO(new Report005().generate(calculationValue, history, currents).toMap()));
            put("shenzhen_gs_q_bit_a_jmqycgwgqyxxbgb", ReportDataDTO.bulidReportDataDTO(new Report006().generate().toMap()));
            put("shenzhen_gs_q_bit_a_jscgtzrgqysdsdynsbab", ReportDataDTO.bulidReportDataDTO(new Report007().generate().toMap()));
        }};
    }


}
