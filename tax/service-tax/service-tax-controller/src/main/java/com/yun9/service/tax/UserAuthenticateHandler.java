//package com.yun9.service.tax;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.yun9.biz.tax.domain.entity.BizTaxProperties;
//import com.yun9.commons.utils.JsonUtils;
//import com.yun9.commons.utils.StringUtils;
//import com.yun9.framework.web.spring.auth.AuthenticateHandler;
//import com.yun9.framework.web.spring.auth.UserDetail;
//import com.yun9.framework.webdriver.http.HttpClientUtils;
//import com.yun9.framework.webdriver.http.WebDriver;
//import com.yun9.service.tax.core.ServiceTaxProperties;
//import com.yun9.service.tax.core.exception.ServiceTaxException;
//import org.apache.http.client.HttpClient;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.io.IOException;
//import java.util.HashMap;
//
//
///**
// * Created by werewolf on  2018/3/26.
// */
//public class UserAuthenticateHandler implements AuthenticateHandler {
//
//    public static final Logger logger = LoggerFactory.getLogger(UserAuthenticateHandler.class);
//
//
//    private final String verifyUrl;
//
//    public UserAuthenticateHandler(String verifyUrl) {
//        this.verifyUrl = verifyUrl;
//    }
//
//    //todo postman测试 添加  header authorization --
//    //todo
//    //Bearer oLGFJuHLk3myLN6weaVt6sXeWawH62mheZ7yN1YLkCTGlQNa8FGxNCoIHlANoNIsg34ICEmw0svXVinsYmIGyK5KFv8H4euYZPwZ0KpiFh0uVunOKDimr7pJVsU97gijUPw5kwJVN5t2OmUMKjbxLCECjZwFFPW3GQCU5hv1wKFseIeSt3iQoCN8CoyKALkQsJzsYmIpeLyEph5AotJXXQRRO8uufaUh62F298pE5b3b0zhH1FrfS1L77u5TUsN|13699429782|10000001463017
//    @Override
//    public UserDetail authenticate(String token) {
//        if (StringUtils.isEmpty(token)) {
//            throw new RuntimeException("用户没用登录");
//        }
//        final String _token = token.split(" ")[1].split("\\|")[0];
//        HttpClient httpClient = HttpClientUtils.build();
//        WebDriver webDriver = new WebDriver(httpClient);
//        try {
//
//            JSONObject jsonObject = webDriver.post(verifyUrl, null,
//                    JSON.toJSONString(new HashMap() {{
//                        put("token", _token);
//                    }})).json(JSONObject.class);
//            if (200 != jsonObject.getInteger("status")) {
//                throw new ServiceTaxException(jsonObject.getString("message"), jsonObject.getInteger("code"));
//            }
//            String body = jsonObject.getString("body");
//            UserDetail userDetail = JsonUtils.parseObject(body, UserDetail.class);
//            if (null == userDetail) {
//                throw new ServiceTaxException("用户认证失败", 403);
//            }
//            return userDetail;
//        } catch (IOException e) {
//            logger.error("用户认证失败 token {}", _token);
//            throw new ServiceTaxException("用户认证失败", 403);
//        }
//    }
//}
