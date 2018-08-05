package com.yun9.service.tax.core.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.Serializable;

@Data
public class TaxDataSyncDTO implements Serializable {

    private String instid; // 当数据来自老版本小微服时，机构ID为老版本机构ID，字符串
    private String taxno;
    private String accountcyclesn;
    private boolean fromOld; // 来自老版本小微服
    private String ftId;
    private JSONObject data;
}
