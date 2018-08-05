package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.service.tax.core.event.NoticeInstEvent;
import com.yun9.service.tax.core.event.ServiceTaxEventPublisher;
import com.yun9.service.tax.core.event.SynScreenShotEvent;
import com.yun9.service.tax.core.v2.OperationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static com.yun9.service.tax.core.v2.annotation.ActionSn.syn_screenshot;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-06-16 15:51
 */
@Component
public class EventService {
    @Autowired
    ServiceTaxEventPublisher serviceTaxEventPublisher;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    /**
     * 异步触发单税种申报后的同步截图事件触发
     *
     * @param bizTaxInstanceCategoryId 税种申报实例id
     * @param createdBy                操作人id
     */
    public void handleSingleTaxDeclaredEventAsync(Long bizTaxInstanceCategoryId, Long createdBy) {
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(bizTaxInstanceCategoryId);
        //如果已申报，发布同步截图事件
        if (bizTaxInstanceCategory.getDeclareType() == DeclareType.taxOffice ||
                bizTaxInstanceCategory.getDeclareType() == DeclareType.yun9) {
            // 发布截图事件
            OperationRequest synScreenShotRequest = new OperationRequest();
            synScreenShotRequest.setTaxInstanceCategoryId(bizTaxInstanceCategoryId);
            synScreenShotRequest.setActionSn(syn_screenshot);
            synScreenShotRequest.setParams(new HashMap<String, Object>(2) {{
                put("synType", "one");
            }});
            synScreenShotRequest.setUserId(createdBy);
            //异步发起同步截图任务
            serviceTaxEventPublisher.publishAsync(new SynScreenShotEvent() {
                {
                    setOperationRequest(synScreenShotRequest);
                }
            });

            // 发布通知机构事件
            serviceTaxEventPublisher.publishAsync(new NoticeInstEvent() {
                {
                    setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
                }
            });
        }
    }
}
