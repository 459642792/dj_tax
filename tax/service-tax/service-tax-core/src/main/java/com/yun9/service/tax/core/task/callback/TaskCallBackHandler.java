package com.yun9.service.tax.core.task.callback;




/**
 * Created by werewolf on  2018/3/28.
 */
public interface TaskCallBackHandler {

    TaskCallBackContext process(TaskCallBackContext context);


}
