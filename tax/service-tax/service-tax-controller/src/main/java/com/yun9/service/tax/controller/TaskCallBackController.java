package com.yun9.service.tax.controller;

import com.yun9.service.tax.core.task.callback.TaskCallBackResponse;
import com.yun9.service.tax.core.task.callback.TaskCallbackFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by werewolf on  2018/3/26.
 * 任务中心回调处理
 */
@RestController
@RequestMapping("task/callback")
public class TaskCallBackController {

    @Autowired
    TaskCallbackFactory taskCallbackFactory;

    /**
     * 任务回调
     *
     * @param response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void callback(@RequestBody TaskCallBackResponse response) {
        taskCallbackFactory.callback(response);
    }
}
