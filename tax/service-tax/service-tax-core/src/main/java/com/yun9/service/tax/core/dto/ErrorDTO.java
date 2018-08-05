package com.yun9.service.tax.core.dto;

import com.yun9.biz.bill.domain.bo.BizBillInvoiceAgentInvoiceDto;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ErrorDTO {
    //公司 会计区间 服务类型
    Map<Integer,String> mapError = new HashMap<>();
    //税额
    List<String> errors = new ArrayList<>();
    //郑的数据结构
    List<BizBillInvoiceAgentInvoiceDto> bizBillInvoiceAgentInvoiceDtos = new ArrayList<>();



    /**
     * 日期 会计区间
     */
    Map<String, Boolean>  cycleMap = new HashMap<>();




}
