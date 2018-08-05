package com.yun9.service.tax.core.task.callback.handler;


import com.yun9.biz.report.BizReportService;
import com.yun9.biz.tax.BizTaxMdCategoryService;
import com.yun9.service.tax.core.task.callback.TaskCallBackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by werewolf on  2018/4/17.
 */
public abstract class AbstractCallbackHandlerMapping implements TaskCallBackHandler {

    private final static Logger logger = LoggerFactory
        .getLogger(AbstractCallbackHandlerMapping.class);


    @Autowired
    protected BizReportService bizReportService;

    @Autowired
    protected BizTaxMdCategoryService bizTaxMdCategoryService;

//    public TaxSn findTaxSnByTaxCode(long taxAreaId, String taxCode) {
//        logger.info("开始查找taxCode{} 对应的 taxSn", taxCode);
//        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findByMdAreaIdAndTaxCode(taxAreaId, taxCode);
//        if (null == bizTaxMdCategory) {
//            logger.error("没有找到taxCode{}对应的税种", taxCode);
//            BizTaxException.throwException(NotSupportTax, taxAreaId, taxCode);
//        }
//        logger.info("找到taxCode :{} 对应的taxSn :{}", taxCode, bizTaxMdCategory.getSn());
//        return bizTaxMdCategory.getSn();
//    }
//
//
//    public BizTaxMdCategory findByTaxCode(long taxAreaId, String taxCode) {
//
//        return bizTaxMdCategoryService.findByMdAreaIdAndTaxCode(taxAreaId, taxCode);
//    }


}
