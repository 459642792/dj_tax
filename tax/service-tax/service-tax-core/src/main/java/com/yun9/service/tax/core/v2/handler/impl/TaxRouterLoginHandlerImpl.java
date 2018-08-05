package com.yun9.service.tax.core.v2.handler.impl;

import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccount;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.taxrouter.request.LoginInfo;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.handler.TaxRouterLoginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-22 14:44
 */
@Component
public class TaxRouterLoginHandlerImpl implements TaxRouterLoginHandler {
    @Autowired
    private BizMdCompanyAccountService bizMdCompanyAccountService;

    @Value("${tax.router.login.platform}")
    private String loginPlatform;
    private final static Logger logger = LoggerFactory.getLogger(TaxRouterLoginHandlerImpl.class);

    @Override
    public LoginInfo getCompanyDefaultAccount(BizMdCompany bizMdCompany, String taxOffice, String areaSn, ActionSn actionSn) {
        LoginInfo loginInfo = this.getCompanyDefaultAccount(bizMdCompany, taxOffice, areaSn);
        if (!(actionSn.name().equals("get_taxes") || ActionSn.syn_company_tax.name().equals(actionSn.name()))) {
            if (loginInfo.getLoginType().equals(BizMdAccount.LoginType.sms.name())) {
                //转税号登录
                logger.debug("当前任务{},登录方式为短信转成税号登录", actionSn.name());
                loginInfo.setLoginType(BizMdAccount.LoginType.taxNo.name());
            }
        }
        return loginInfo;
    }
    @Override
    public LoginInfo getCompanyDefaultAccount(BizMdCompany bizMdCompany, String taxOffice, String areaSn) {
        CompanyAccountDTO companyAccountDTO = bizMdCompanyAccountService.findActivateByCompanyIdAndType(bizMdCompany.getId(), BizMdAccount.Type.valueOf(taxOffice));

        if (null == companyAccountDTO) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.company_has_not_default_account);
        }
        if (companyAccountDTO.getLoginType() == BizMdAccount.LoginType.gsjump) {
            companyAccountDTO = bizMdCompanyAccountService.findActivateByCompanyIdAndType(bizMdCompany.getId(), BizMdAccount.Type.gs);
        }

        if (StringUtils.isEmpty(bizMdCompany.getFullName())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskStartFailed, "客户名称不能为空");
        }
        if (StringUtils.isEmpty(companyAccountDTO.getLoginAccount()) || StringUtils.isEmpty(companyAccountDTO.getLoginPassword())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskStartFailed, "账号或者密码为空");
        }

        if (companyAccountDTO.getLoginType().equals(BizMdAccount.LoginType.sms) && StringUtils.isEmpty(companyAccountDTO.getLoginPhoneNum())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TaskStartFailed, "登录方式为短信,手机号不能为空");
        }

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setCompanyName(bizMdCompany.getFullName());
        loginInfo.setLoginType(companyAccountDTO.getLoginType().name());
        loginInfo.setPassword(companyAccountDTO.getLoginPassword());
        loginInfo.setPhoneNum(companyAccountDTO.getLoginPhoneNum());
        loginInfo.setTaxArea(areaSn);
        loginInfo.setTaxNo(bizMdCompany.getTaxNo());
        loginInfo.setUserNo(companyAccountDTO.getLoginAccount());
        loginInfo.setLoginPlatform(loginPlatform);
        return loginInfo;
    }


}
