package com.yun9.service.tax.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.md.BizMdAreaService;
import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccount;
import com.yun9.biz.md.domain.entity.BizMdArea;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.ServiceTaxProperties;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.utils.OkHttpUtils;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 直连税务路由控制器
 */
@Controller
@RequestMapping("/taxoffice")
public class TaxOfficeController {

    public static final Logger logger = LoggerFactory.getLogger(TaxOfficeController.class);

    public static final String URL_FOR_PERSONAL = "shengzhen_gs_personal_web";

    public static final String URL_FOR_TAXNO = "shengzhen_oldgs_taxno_weixin";

    public static final String URL_FOR_DS_PERSONAL = "shengzhen_gds_personal_web";

    public static final String URL_FOR_DS_TAXNO = "shengzhen_gds_taxno_weixin";


    private static final Map<String, String> officeTargeUrlMap = new HashMap() {{
        this.put("shenzhen_ds", "https://dzswj.szds.gov.cn/dzswj/gdyth.do?method=toGnmk&flag=gypt&ticket=");
        this.put("shenzhen_gs_new", "https://dzswj.sztax.gov.cn/bszm-web/BszmWeb/views/index.html");
        this.put("shenzhen_gs_old","http://dzswj.szgs.gov.cn/BsfwtWeb/apps/views/myoffice/myoffice.html");
    }};

    @Autowired
    private BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    private ServiceTaxProperties serviceTaxProperties;

    @Autowired
    private BizMdCompanyService bizMdCompanyService;

    @Autowired
    BizMdAreaService bizMdAreaService;

    /**
     * 通过公司ID获取登录cookie
     *
     * @param companyId
     * @param taxOffice  gs ds
     * @param userDetail
     */
    @RequestMapping(value = "/cookie/{companyId}/{taxOffice}", method = RequestMethod.GET)
    @ResponseBody
    public Object getCookie(@PathVariable long companyId,
                            @PathVariable BizMdAccount.Type taxOffice,
                            @User UserDetail userDetail) {
        CompanyAccountDTO gsLoginInfo = bizMdCompanyAccountService.findActivateByCompanyIdAndType(companyId, BizMdAccount.Type.gs);
        if (null == gsLoginInfo || null == gsLoginInfo.getLoginType()) {
            throw new ServiceTaxException("无法找到当前客户的国税登录信息");
        }
        BizMdCompany bizMdCompany = bizMdCompanyService.findById(companyId);

        JSONObject requestBody = new JSONObject();
        String getCookieUrl = serviceTaxProperties.getGetCookieUrl();
        if (taxOffice == BizMdAccount.Type.gs) {
            if (BizMdAccount.LoginType.personal == gsLoginInfo.getLoginType()) {
                getCookieUrl +=  URL_FOR_PERSONAL;
            } else {
                getCookieUrl +=  URL_FOR_TAXNO;
            }
        } else {
            if (BizMdAccount.LoginType.personal == gsLoginInfo.getLoginType()) {
                getCookieUrl +=  URL_FOR_DS_PERSONAL;
            } else {
                getCookieUrl +=  URL_FOR_DS_TAXNO;
            }
        }
        requestBody.put("password", gsLoginInfo.getLoginPassword());
        requestBody.put("account", gsLoginInfo.getLoginAccount());
        requestBody.put("ids", new JSONArray(){{
            this.add(bizMdCompany.getTaxNo());
        }});
        requestBody.put("loginType", gsLoginInfo.getLoginType());
        requestBody.put("phoneNum", gsLoginInfo.getLoginPhoneNum());
        requestBody.put("extras", new JSONObject());
        logger.info("请求cookie接口地址：" + getCookieUrl);
        logger.info("请求cookie参数：" + requestBody.toJSONString());
        OkHttpClient okHttpClient = OkHttpUtils.getClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestBody.toJSONString());
        Request request = new Request.Builder()
                .url(getCookieUrl)
                .post(body)
                .addHeader("content-type", "application/json")
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();
            response.close();
            JSONObject responseJson = JSONObject.parseObject(responseBody);
            if (!responseJson.containsKey("body") || !responseJson.getJSONObject("body").containsKey("data") ||
                    responseJson.getJSONObject("body").get("data") == null ) {
                logger.info("获取cookie返回内容："+responseBody);
                throw new ServiceTaxException("登录失败,"+responseJson.getJSONObject("body").getString("message"));
            }
            JSONObject responseBodyJson = responseJson.getJSONObject("body").getJSONObject("data");
            responseBodyJson.put("companyName",bizMdCompany.getFullName());
            responseBodyJson.put("loginType",gsLoginInfo.getLoginType());
            responseBodyJson.put("targetUrls", JSONObject.toJSON(officeTargeUrlMap));
            return responseBodyJson;
        } catch (IOException e) {
            throw new ServiceTaxException("请求税务路由异常：" + e.getMessage());
        }
    }
}
