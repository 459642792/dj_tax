package com.yun9.service.tax.controller;

import com.yun9.commons.exception.ForbiddenException;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.v2.OperationRequest;
import com.yun9.service.tax.core.v2.TaxStartFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Optional;

/**
 * Created by werewolf on  2018/4/18.
 * 所有税种操作
 */
//@RestController
@Controller
@RequestMapping("task/start")
public class TaskStartController {

    @Autowired
    private TaxStartFactory taxStartFactory;

    @PostMapping //(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object start(@RequestBody @Valid OperationRequest request,
                        @User UserDetail userDetail) {
        //用户id，用当前机构用户ID
        request.setUserId(Optional.ofNullable(userDetail.getInstUserId()).orElseThrow(() -> new ForbiddenException("不存在当前机构和用户关系")));
        return taxStartFactory.handler(request);
    }
}
