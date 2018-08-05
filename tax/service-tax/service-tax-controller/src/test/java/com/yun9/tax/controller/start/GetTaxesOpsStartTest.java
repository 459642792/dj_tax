//package com.yun9.tax.controller.start;
//
//import com.yun9.biz.task.BizTaskService;
//import com.yun9.biz.tax.enums.CycleType;
//import com.yun9.biz.tax.enums.TaxOffice;
//import com.yun9.service.tax.controller.TaskStartController;
//import com.yun9.service.tax.core.task.start.TaskStartBO;
//import com.yun9.service.tax.core.task.start.context.TaskStartType;
//import com.yun9.tax.controller.BaseTest;
//import org.junit.After;
//import org.junit.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//
///**
// * 多税种下载发起任务测试用例
// *
// * @author lvpanfeng
// * @version 1.0
// * @since 2018-04-28 13:52
// */
//
//public class GetTaxesOpsStartTest extends BaseTest {
//    @Autowired
//    TaskStartController taskStartController;
//    @Autowired
//    BizTaskService bizTaskService;
//    private String seq;
//
//    @Test
//    public void startGetTaxesOps() throws Exception {
//        MvcResult result = mvc.perform(
//                MockMvcRequestBuilders.post("/task/start")
//                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
//                        .content(mapper.writeValueAsString(new TaskStartBO()
//                                .setInstId(1)
//                                .setUserId(1)
//                                .setAccountCycleId(96)
//                                .setCompanyId(22)
//                                .setTaxOffice(TaxOffice.gs)
//                                .setCycleType(CycleType.y)
//                                .setTaskStartType(TaskStartType.GET_TAXES))))
//                .andDo(print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//        //TODO result 对应业务自己的操作判断是否
//        String body = result.getResponse().getContentAsString();
//        seq = body;
//        System.out.println("body:" + body);
//    }
//
//    @After
//    public void after() throws Exception {
//        bizTaskService.cancel(seq, "test", "test");
//    }
//
//    @Override
//    public Object controller() {
//        return taskStartController;
//    }
//}
