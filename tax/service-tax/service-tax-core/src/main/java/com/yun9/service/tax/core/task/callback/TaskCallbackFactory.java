package com.yun9.service.tax.core.task.callback;

/**
 * 处理来自任务中心的任务完成回调
 */
public interface TaskCallbackFactory {
    void callback(TaskCallBackResponse callBackResponse);
}
