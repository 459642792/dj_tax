package com.yun9.service.tax.core.taxrouter.request;

import com.yun9.service.tax.core.taxrouter.request.LoginInfo;
import lombok.Data;

import java.util.Map;

/**
 * Created by werewolf on  2018/4/19.
 */
@Data
public class TaxRouterRequest {

    private LoginInfo loginInfo;

    private Map<String,Object> params;


}
