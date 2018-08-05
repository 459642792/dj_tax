package com.yun9.service.tax.core.report.sz.personal_business_q;

import com.yun9.biz.md.BizMdDictionaryCodeService;
import com.yun9.biz.md.domain.entity.BizMdDictionaryCode;
import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.tax.BizTaxInstanceCategoryDataService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalBusinessItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalBusinessService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryData;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalBusiness;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalBusinessItem;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.report.IReportGenerate;
import com.yun9.service.tax.core.report.ReportFactory;
import com.yun9.service.tax.core.report.ReportSnMapping;
import com.yun9.service.tax.core.report.sz.personal_business_q.report.Report001;
import com.yun9.service.tax.core.report.sz.personal_business_q.report.Report002;
import com.yun9.service.tax.core.report.sz.personal_business_q.report.Report003;
import com.yun9.service.tax.core.task.helper.JsonMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 深圳生成经营季报
 */
@ReportSnMapping(sns = "shenzhen_ds_q_scjy_a")
public class GsQPersonalBusinessGenerate implements IReportGenerate {

    @Autowired
    private BizTaxInstanceCategoryDataService bizTaxInstanceCategoryDataService;

    @Autowired
    private BizTaxInstanceCategoryPersonalBusinessService bizTaxInstanceCategoryPersonalBusinessService;

    @Autowired
    private BizTaxInstanceCategoryPersonalBusinessItemService bizTaxInstanceCategoryPersonalBusinessItemService;

    @Autowired
    private BizMdDictionaryCodeService bizMdDictionaryCodeService;

    @Override
    public Map<String, List<ReportDataDTO>> generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body) {

        if (null ==  body || !body.containsKey(ReportFactory.PARAM_REPORT_ITEM_ID)) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.IllegalArgumentException,ReportFactory.PARAM_REPORT_ITEM_ID);
        }

        BizTaxInstanceCategoryData currentData = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.current, bizTaxInstanceCategory.getId());

        if (null == currentData) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.report_current_data_not_found);
        }

        //当前数据
        final Map<String, String> currents = currentData.toMap();
        // 证件类型
        final Map<String, String> cardTypes = new HashMap<>();
        // 证件类型
        final Map<String, String> countryTypes = new HashMap<>();

        List<BizMdDictionaryCode> cardTypeCodes = bizMdDictionaryCodeService.findByDefsn("cardtype");
        List<BizMdDictionaryCode> countryCodes = bizMdDictionaryCodeService.findByDefsn("country");
        cardTypeCodes.stream().forEach((item) -> {
            cardTypes.put(item.getSn(), item.getName().substring(4, item.getName().length()));
        });
        countryCodes.stream().forEach((item) -> {
            countryTypes.put(item.getSn(), item.getName().substring(4, item.getName().length()));
        });

        Report001.CalculationValue calculationValue = new Report001.CalculationValue();

        BizTaxInstanceCategoryPersonalBusiness business = bizTaxInstanceCategoryPersonalBusinessService.findByInstanceCategoryId(bizTaxInstanceCategory.getBizTaxInstanceId());
        //营业收入
        calculationValue.incomeProfitAmount = business.getIncomeAmount();
        //营业成本
        calculationValue.costProfitAmount = business.getBuyAmount();
        //利润总额
        calculationValue.profitProfitAmount = business.getProfitAmount();
        //减除费用
        calculationValue.deductionAmount = business.getDeductionAmount();

        BizTaxInstanceCategoryPersonalBusinessItem personalBusinessItem = bizTaxInstanceCategoryPersonalBusinessItemService.
                findById(Long.valueOf(body.getString(ReportFactory.PARAM_REPORT_ITEM_ID)));


        Map<String, Object> report001 = new Report001().generate(calculationValue,personalBusinessItem,
                cardTypes, countryTypes,currents).toMap();

        return new HashMap() {{
            put("shenzhen_ds_q_scjy_a_scjysdgssbabzb", ReportDataDTO.bulidReportDataDTO(report001));
            put("shenzhen_ds_q_scjy_a_grsdsjmssxbgb", new Report002().generate().toMap());
            put("shenzhen_ds_q_scjy_a_syjkbxsqkcb", new Report003().generate().toMap());
        }};
    }

    @Override
    public boolean isResetCreate() {
        return true;
    }

}
