package com.yun9.service.tax.core;


import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFzItem;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.dto.BizTaxFzItemDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:43
 **/
public interface TaxInstanceCategoryFzFactory {


    /**
     * 根据状态获取分页查询
     *
     * @param accountCycleIds 会计区间ID
     * @param orgTreeId       组织ID
     * @param state           状态
     * @param page            页数
     * @param limit           每页条数
     * @param params          过滤参数
     * @return
     */
    Pagination<HashMap> pageByState(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state, int page, int limit, Map<String, Object> params);

    /**
     * 
     * 审核确认
     */
    void confirmed(BizTaxFzItemDTO bizTaxFzItemDTO,long userId);
    /**
     * 征收品目列表
     * @param instanceCategoryFzId
     * @return
     */
    Optional<List<BizTaxInstanceCategoryFzItem>> itemList(long instanceCategoryFzId);

    /**
     * 保存品目
     * @param instanceCategoryFzId
     * @param itemList
     * @return
     */
    List<BizTaxInstanceCategoryFzItem> saveItem(long instanceCategoryFzId, List<BizTaxInstanceCategoryFzItem> itemList) throws IllegalAccessException;

    void unconfirmed(long bizTaxInstanceCategoryFzId,long userId);
    
    Map<String,Object> getVatAndSoq(long bizTaxInstanceCategoryFzId);
    
    void batchAudit(List<Long> ids, long userId);
}
