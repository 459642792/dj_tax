package com.yun9.service.tax.core.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.io.Serializable;

@Data
public class TaxDataUpdateClientDTO implements Serializable {

    private String instid; // 当数据来自老版本小微服时，机构ID为老版本机构ID，字符串
    private String selecttype;
    private boolean fromOld; // 来自老版本小微服
    private JSONArray clients;
}
