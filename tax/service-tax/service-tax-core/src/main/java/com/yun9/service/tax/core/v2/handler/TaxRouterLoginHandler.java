package com.yun9.service.tax.core.v2.handler;

import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.service.tax.core.taxrouter.request.LoginInfo;
import com.yun9.service.tax.core.v2.annotation.ActionSn;

/**
 * Created by werewolf on  2018/5/22.
 */
public interface TaxRouterLoginHandler {

    LoginInfo getCompanyDefaultAccount(BizMdCompany bizMdCompany, String taxOffice, String areaSn, ActionSn actionSn);


    @Deprecated
    LoginInfo getCompanyDefaultAccount(BizMdCompany bizMdCompany, String taxOffice, String areaSn);
}
