package com.yun9.service.tax.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by werewolf on  2018/4/8.
 */
@Data
@ConfigurationProperties(prefix = "service.tax")
@Configuration
public class ServiceTaxProperties {

    private String taskEnv; //任务环境
    private String taskCallBackUrl;// 任务回调url
    private String verifyUrl; //token 验证url
    private String getCookieUrl; // 税务路由获取cookie接口地址

    private String taskUrl;

    public String buildTaskUrl(String taskSn, String processBy) {
//        http://test.yun9.com:8761/api/task/lifecycle/start/{env}/sn/{taskSn}/{processBy}
        //        http://test.yun9.com:8761/api/task/mock/start/{env}/sn/{taskSn}/{processBy}
        return String.format(this.taskUrl, this.taskEnv, taskSn, processBy);
    }

    public boolean isMock() {
        return false;
    }

}
