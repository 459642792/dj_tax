package com.yun9.service.tax;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yun9.commons.exception.ForbiddenException;
import com.yun9.commons.utils.JsonUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.web.spring.auth.AuthenticateHandler;
import com.yun9.framework.web.spring.auth.DefaultUserAuthenticate2;
import com.yun9.framework.web.spring.auth.UserDetail;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class OkAuthenticateHandler implements AuthenticateHandler {

    public static final Logger logger = LoggerFactory.getLogger(OkAuthenticateHandler.class);

    private final String verifyUrl;

    private static OkHttpClient client;

    public OkAuthenticateHandler(String verifyUrl) {
        this.verifyUrl = verifyUrl;
    }

    public static OkHttpClient getClient() {
        if (client == null) {
            synchronized (OkAuthenticateHandler.class) {
                if (client == null) {
                    ConnectionPool pool = new ConnectionPool(10000, 10, TimeUnit.MINUTES);
                    client = new OkHttpClient.Builder()
                            .retryOnConnectionFailure(true)
                            .readTimeout(2, TimeUnit.MINUTES)
                            .writeTimeout(3, TimeUnit.MINUTES)
                            .connectionPool(pool)
                            .connectTimeout(3, TimeUnit.MINUTES) //
                            .build();
                }
            }
        }
        return client;
    }

    @Override
    public UserDetail authenticate(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new ForbiddenException("用户没用登录");
        } else {
            final String[] _token = token.split(" ")[1].split("\\|");
            try {
                if (StringUtils.isEmpty(_token[0])) {
                    logger.debug("没有获取到用户登录的token{}", token);
                    throw new ForbiddenException("用户没用登录");
                } else {
                    OkHttpClient okHttpClient = OkAuthenticateHandler.getClient();
                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody requestBody = RequestBody.create(mediaType, JSON.toJSONString(new HashMap() {
                        {
                            this.put("token", _token[0]);
                        }
                    }));
                    Request request = new Request.Builder()
                            .url(this.verifyUrl)
                            .post(requestBody)
                            .addHeader("content-type", "application/json")
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    String responseBody = response.body().string();
                    response.close();
                    okHttpClient.connectionPool().evictAll();
                    JSONObject jsonObject = JSONObject.parseObject(responseBody);
                    if (200 != jsonObject.getInteger("code")) {
                        logger.debug("登录返回参数{}", jsonObject);
                        throw new ForbiddenException(jsonObject.getString("message") == null ? "用户登录失败!" : jsonObject.getString("message"));
                    } else {
                        DefaultUserAuthenticate2.ResponseData responseData = (DefaultUserAuthenticate2.ResponseData)JsonUtils.parseObject(jsonObject.getString("data"), DefaultUserAuthenticate2.ResponseData.class);
                        UserDetail userDetail = new UserDetail();
                        if (null != responseData.getUserInfo() && null != responseData.getUserInfo().getUser()) {
                            userDetail.setId(responseData.getUserInfo().getUser().getId());
                            userDetail.setName(responseData.getUserInfo().getUser().getName());
                            if (_token.length > 1 && StringUtils.isNotEmpty(_token[1])) {
                                userDetail.setInstId(Long.valueOf(_token[1]));
                            }

                            if (_token.length > 2 && StringUtils.isNotEmpty(_token[2])) {
                                userDetail.setInstUserId(Long.valueOf(_token[2]));
                            }

                            return userDetail;
                        } else {
                            logger.debug("登录认证返回信息{}", jsonObject);
                            throw new ForbiddenException("用户认证失败");
                        }
                    }
                }
            } catch (IOException var7) {
                logger.error("发生异常", var7);
                throw new ForbiddenException("error", var7);
            }
        }
    }

}
