package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaxDataUnAuditDTO implements Serializable {

    public enum TaxType {
        personaltax,vat,bit,frq;
    }

    private String instid; // 当数据来自老版本小微服时，机构ID为老版本机构ID，字符串
    private String taxno;
    private String accountcyclesn;
    private boolean fromOld; // 来自老版本小微服
    private String ftId;
    /**
     *
     * "personaltax"："个人所得税",
     *  "vat"：              "增值税"
     *  "bit":                  "企业所得税"
     *  "frq":                 "财务报表"
     *
     */
    private String taxtype;
}
