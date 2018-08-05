package com.yun9.service.tax.core.taxrouter.response;

import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表数据
 *
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-03-21 15:14
 */
@Data
public class ReportDTO {
    /**
     * 报表唯一标识
     */
    private String sheetSn;
    /**
     * 报表名称
     */
    private String name;
    /**
     * 报表数据
     */
    private List<ReportDataDTO> reportDataDTOS;



}
