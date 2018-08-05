package com.yun9.tax.controller.callback;

import com.alibaba.fastjson.JSON;
import com.yun9.service.tax.controller.MockBaseTest;
import com.yun9.service.tax.controller.TaskCallBackController;
import com.yun9.service.tax.core.task.callback.TaskCallBackResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-07 16:09
 */

public class DeclareResultOpsCallbackTest extends MockBaseTest {
    @Autowired
    TaskCallBackController taskCallBackController;

    @Test
    public void callback() throws Exception{
        String requestBody = "{\"accountCycleSn\":\"201712\",\"bizId\":\"123\",\"body\": [\n" +
                "  {\n" +
                "    \"taxCode\":\"bita\",\n" +
                "    \"alreadyDeclared\":\"N\",\n" +
                "    \"picList\":[\n" +
                "      {\n" +
                "        \"type\":\"declare\",\n" +
                "        \"fileType\":\"main\",\n" +
                "        \"url\":\"http://screenshot-tax-result.oss-cn-shenzhen.aliyuncs.com/1522325291511/63823620e2a84b61961145b225a353a8.png\"\n" +
                "      }\n" +
                "\n" +
                "]\n" +
                "}\n" +
                "],\"code\":200,\"instId\":\"1\",\"message\":\"message\",\"seq\":\"df7f215123224132972072ed019a87db\",\"sn\":\"SZ0015\"}";
        TaskCallBackResponse taskCallBackResponse = JSON.parseObject(requestBody,TaskCallBackResponse.class);

        MvcResult result = mvc.perform(
                MockMvcRequestBuilders.post("/task/callback")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(mapper.writeValueAsString(taskCallBackResponse)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        //TODO result 对应业务自己的操作判断是否
        result.getResponse().setCharacterEncoding("UTF-8");
        String body = result.getResponse().getContentAsString();
        System.out.println("body:" + body);

    }

    @Override
    public Object controller() {
        return taskCallBackController;
    }
}
