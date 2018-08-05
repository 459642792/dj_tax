package com.yun9.service.tax.core.dto;

import java.util.List;
import lombok.Data;

/**
 * @author huanglei
 * @version 1.0
 * @Description: TODO
 * @since 2018-06-02 18:04
 */
@Data
public class BizTaxBitItemDTO {
    private String company;
    private String accountCycle;
    private String szlx;
    private String hdlx;
    private String income;
    private String cost;
    private String profit;
    private String ynse;
    //是否科技型中小企业
    private String technologySmallCompany;
    //是否高新技术企业
    private String highTechnologyCompany;
    //是否技术入股递延纳税事项
    private String technologyAdmissionMatter;
    //期末从业人数
    private String employeeNumber;
    private long id;
    private String code;
    private List<String> message;
}
