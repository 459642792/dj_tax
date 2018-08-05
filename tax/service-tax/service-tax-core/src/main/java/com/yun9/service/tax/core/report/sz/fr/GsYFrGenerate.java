package com.yun9.service.tax.core.report.sz.fr;

import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.service.tax.core.report.IReportGenerate;
import com.yun9.service.tax.core.report.ReportSnMapping;
import com.yun9.service.tax.core.task.helper.JsonMap;

import java.util.List;
import java.util.Map;

/**
 * Created by werewolf on  2018/6/8.
 */
@ReportSnMapping(sns = {"shenzhen_gs_y_small_fr", "shenzhen_gs_y_normal_fr"})
public class GsYFrGenerate implements IReportGenerate {
    @Override
    public Map<String, List<ReportDataDTO>> generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body) {
        return null;
    }
}
