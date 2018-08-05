package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaxDataNoticeInstDTO implements Serializable {

    private long initId; // 指定机构ID
    private Long companyId; // 指定客户ID
    private long mdAccountCycleId; // 税期ID
    private String taxOffice; // 国地税标识 gs ds
}
