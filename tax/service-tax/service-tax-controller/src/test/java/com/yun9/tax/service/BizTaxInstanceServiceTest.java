//package com.yun9.tax.service;
//
//import com.yun9.biz.tax.BizTaxInstanceService;
//import com.yun9.biz.tax.domain.entity.BizTaxInstance;
//import com.yun9.biz.tax.enums.TaxOffice;
//import com.yun9.framework.orm.commons.criteria.Pagination;
//import com.yun9.service.tax.controller.BaseTest;
//import org.junit.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.HashMap;
//
///**
// * @author lvpanfeng
// * @version 1.0
// * @since 2018-04-30 17:36
// */
//
//public class BizTaxInstanceServiceTest extends BaseTest {
//    @Autowired
//    BizTaxInstanceService bizTaxInstanceService;
//
//    @Test
//    public void test() {
//        Pagination<BizTaxInstance> declareInfoDTOPageBean = bizTaxInstanceService.findDeclareInfoPage(1, 5, new HashMap() {{
//            put("mdAreaId", 1);
//            put("taxOffice", TaxOffice.gs);
//            put("taxType", "personal");
//            put("downloadState", BizTaxInstance.DownloadState.success);
//        }});
//        System.out.println();
//    }
//}
