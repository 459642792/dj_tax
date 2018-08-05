package com.yun9.service.tax;

import com.yun9.framework.web.spring.ApplicationConfigureAdapter;
import com.yun9.framework.web.spring.interceptor.ResponseBodyVoidInterceptor;
import com.yun9.framework.web.spring.interceptor.TimeLogInteceptor;
import com.yun9.framework.web.spring.interceptor.TokenInterceptor;
import com.yun9.framework.web.spring.resolver.PageParamArgumentResolver;
import com.yun9.framework.web.spring.resolver.QueryParamArgumentResolver;
import com.yun9.framework.web.spring.resolver.UserArgumentResolver;
import com.yun9.service.tax.core.ServiceTaxProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

/**
 * Created by werewolf on  2018/3/13.
 */
@SpringBootApplication
@ImportResource("classpath:spring-context.xml")
@EnableDiscoveryClient
public class TaxApplication extends ApplicationConfigureAdapter {

    public static void main(String[] args) {
        SpringApplication.run(TaxApplication.class, args);
    }


    @Autowired
    ServiceTaxProperties serviceTaxProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new PageParamArgumentResolver());
        argumentResolvers.add(new QueryParamArgumentResolver());
        argumentResolvers.add(new UserArgumentResolver()); //todo warning
        super.addArgumentResolvers(argumentResolvers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TimeLogInteceptor());
        registry.addInterceptor(new ResponseBodyVoidInterceptor());
//        registry.addInterceptor(new TokenInterceptor(new DefaultUserAuthenticate(serviceTaxProperties.getVerifyUrl())))
//                .excludePathPatterns("/task/callback/**");

        //todo 新版单点登录方式
        //修改配置文件单点登录认证url todo getVerifyUrl 这个参数
        //获得当前（机构用户ID）方式 userDetail.getInstUserId()
        registry.addInterceptor(new TokenInterceptor(new OkAuthenticateHandler(serviceTaxProperties.getVerifyUrl())))
                .excludePathPatterns("/task/callback/**");

        super.addInterceptors(registry);
    }

}
