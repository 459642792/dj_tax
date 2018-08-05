package com.yun9.service.tax.core.report;

import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.service.tax.core.task.helper.JsonMap;

import java.util.List;
import java.util.Map;

/**
 * Created by werewolf on  2018/6/1.
 */
public interface IReportGenerate {

    /**
     * 生成报表
     *
     * @param bizTaxInstanceCategory
     * @param body
     * @return
     */
    Map<String, List<ReportDataDTO>> generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body);


    /**
     * 是否重新创建报表
     *
     * @return
     */
    default boolean isResetCreate() {
        return false;
    }
}
