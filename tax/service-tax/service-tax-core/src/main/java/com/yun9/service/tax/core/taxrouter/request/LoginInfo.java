package com.yun9.service.tax.core.taxrouter.request;

import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccount;
import com.yun9.biz.md.domain.entity.BizMdArea;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import lombok.Data;

/**
 * Created by werewolf on  2018/4/19.
 */
@Data
public class LoginInfo {
    private String userNo;
    private String password;
    private String taxArea;
    private String taxNo;
    private String loginPlatform;
    private String loginType;
    private String companyName;
    private String phoneNum;

    public static LoginInfo build(CompanyAccountDTO companyAccountDTO, BizMdCompany bizMdCompany, BizMdArea bizMdArea) {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setCompanyName(bizMdCompany.getFullName());
        loginInfo.setLoginType(companyAccountDTO.getLoginType().name());
        loginInfo.setPassword(companyAccountDTO.getLoginPassword());
        loginInfo.setPhoneNum(companyAccountDTO.getLoginPhoneNum());
        loginInfo.setTaxArea(bizMdArea.getSn());
        loginInfo.setTaxNo(bizMdCompany.getTaxNo());
        loginInfo.setUserNo(companyAccountDTO.getLoginAccount());
        return loginInfo;
    }
}
