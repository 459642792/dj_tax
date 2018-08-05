package com.yun9.service.tax.core.task.helper;

import com.yun9.commons.utils.JsonUtils;
import com.yun9.commons.utils.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * Created by werewolf on  2018/5/21.
 */
public class JsonMap extends HashMap<String, Object> implements Serializable {

    private Object value(String key) {
        return super.get(key);
    }

    public Integer getInteger(String key) {
        return this.getInteger(key, null);
    }

    public Integer getInteger(String key, Integer defaultVal) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return Integer.valueOf(obj.toString());
        }
        return defaultVal;
    }

    public boolean getBoolean(String key) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return Boolean.valueOf(obj.toString());
        }
        return false;
    }
    public Long getLong(String key) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return Long.valueOf(obj.toString());
        }
        return null;
    }

    public String getString(String key) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return obj.toString();
        }
        return null;
    }
    public String getString(String key,String defaultValue) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return obj.toString();
        }
        return defaultValue;
    }
    public BigDecimal getBigDecimal(String key) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return new BigDecimal(obj.toString());
        }
        return new BigDecimal("0.00");
    }

    public JsonMap getObject(String key) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return JsonUtils.parseObject(obj.toString(), JsonMap.class);
        }
        return null;
    }

    public List<JsonMap> getArray(String key) {
        Object obj = this.value(key);
        if (StringUtils.isNotEmpty(obj)) {
            return JsonUtils.parseArray(obj.toString(), JsonMap.class);
        }
        return null;
    }
}
