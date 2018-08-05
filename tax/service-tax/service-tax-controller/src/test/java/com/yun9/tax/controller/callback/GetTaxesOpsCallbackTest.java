package com.yun9.tax.controller.callback;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.task.BizTaskService;
import com.yun9.service.tax.controller.TaskCallBackController;
import com.yun9.service.tax.core.task.callback.TaskCallBackResponse;
import com.yun9.tax.controller.BaseTest;
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
 * @since 2018-04-28 16:00
 */

public class GetTaxesOpsCallbackTest extends BaseTest {
    @Autowired
    BizTaskService bizTaskService;
    private String seq;
    @Autowired
    TaskCallBackController taskCallBackController;

    @Test
    public void callbackGetTaxesOps() throws Exception {

        String requestBody = "{\"accountCycleSn\":\"201712\",\"bizId\":\"123\",\"body\": [\n" +
                "        {\n" +
                "            \"closeDate\": 0,\n" +
                "            \"taxCustomData\": {},\n" +
                "            \"endDate\": 1514735999000,\n" +
                "            \"name\": \"《中华人民共和国企业年度关联业务往来汇总表》\",\n" +
                "            \"alreadyDeclared\": \"N\",\n" +
                "            \"taxCode\": \"10437_10437\",\n" +
                "            \"startDate\": 1483200000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"closeDate\": 0,\n" +
                "            \"taxCustomData\": {},\n" +
                "            \"endDate\": 1514735999000,\n" +
                "            \"name\": \"企业所得税年度纳税申报A类\",\n" +
                "            \"alreadyDeclared\": \"N\",\n" +
                "            \"taxCode\": \"10440_10440\",\n" +
                "            \"startDate\": 1483200000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"closeDate\": 0,\n" +
                "            \"taxCustomData\": {\n" +
                "                \"taxRecord\": \"Y\"\n" +
                "            },\n" +
                "            \"endDate\": 1514735999000,\n" +
                "            \"taxPaySn\": \"\",\n" +
                "            \"name\": \"一般企业财务报表-年报\",\n" +
                "            \"alreadyDeclared\": \"Y\",\n" +
                "            \"taxCode\": \"29812_29812\",\n" +
                "            \"startDate\": 1483200000000\n" +
                "        }\n" +
                "    ],\"code\":200,\"instId\":\"1\",\"message\":\"message\",\"seq\":\"81efcb19377b4b8187263992a53c301f\",\"sn\":\"SZ0015\"}";
        TaskCallBackResponse taskCallBackResponse = JSON.parseObject(requestBody, TaskCallBackResponse.class);

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
