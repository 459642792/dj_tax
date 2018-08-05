package com.yun9.service.tax.core.taxrouter.response;

import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxSn;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by werewolf on  2018/3/21.
 * 申报清册返回数据
 */
@Data
public class DeclareBitResponse {


    private Long sendDate;
    private DeclareType declareType;
    private String taxPaySn;
    private List<Inventory> linkedHistory;


    //汇算清册结构
    @Data
    public static class Inventory {
        private String taxCode;
        private DeclareResponse.AlreadyDeclared alreadyDeclared;
        private TaxSn taxSn;
        private Long beginDate;
        private Long endDate;
        private String taxPaySn;
        private BigDecimal taxPayAmount;
        private CycleType cycleType;
        private Long closeDate;
        private Long sendDate;
        private String name;

        //报表结构
        private DeclareResponse.History history;
    }


}
