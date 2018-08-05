package com.yun9.service.tax.core.task.callback;

import lombok.Data;

/**
 * Created by werewolf on  2018/4/17.
 */
@Data
public class TaskCallBackResponse {
    private String body;
    private String instId;
    private String bizId;
    private String sn;
    private String seq;
    private Integer code;
    private String message;
    private String accountCycleSn;
}


