//package com.yun9.tax.controller.start;
//
//import com.yun9.biz.task.BizTaskService;
//import com.yun9.biz.tax.enums.CycleType;
//import com.yun9.biz.tax.enums.TaxOffice;
//import com.yun9.service.tax.controller.TaskStartController;
//import com.yun9.service.tax.core.task.start.TaskStartBO;
//import com.yun9.service.tax.core.task.start.context.TaskStartType;
//import com.yun9.tax.controller.BaseTest;
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
// * @author lvpanfeng
// * @version 1.0
// * @since 2018-04-28 15:55
// */
//
//public class GetTaxOpsStartTest extends BaseTest {
//    @Autowired
//    TaskStartController taskStartController;
//    @Autowired
//    BizTaskService bizTaskService;
//    private String seq;
//
//    @Test
//    public void startGetTaxOps() throws Exception {
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
//                                .setTaxInstanceCategoryId(64L)
//                                .setTaskStartType(TaskStartType.GET_TAX))))
//                .andDo(print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//        //TODO result 对应业务自己的操作判断是否
//        String body = result.getResponse().getContentAsString();
//        seq = body;
//        System.out.println("body:" + body);
//    }
//
//
//    @Override
//    public Object controller() {
//        return taskStartController;
//    }
//}
