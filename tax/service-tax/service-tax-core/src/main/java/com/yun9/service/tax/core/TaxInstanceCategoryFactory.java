package com.yun9.service.tax.core;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.*;
import com.yun9.framework.orm.commons.criteria.Pagination;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhengzb on 2018/5/7.
 */
public interface TaxInstanceCategoryFactory {

    /**
     * 统计异常状态下的用户数量
     *
     * @param orgTreeId 组织节点id
     * @param state
     * @param params
     * @return
     */
    HashMap totalByException(long orgTreeId, String state, HashMap<String, Object> params);

    /**
     * 未申报列表
     *
     * @param orgTreeId
     * @param page
     * @param limit
     * @param params
     * @return
     */
    Pagination listLaunch(long orgTreeId, int page, int limit, HashMap<String, Object> params);

    /**
     * 统计各种状态下客户数量（所有税种通用）
     *
     * @param orgTreeId
     * @param processStates
     * @param params
     * @return
     */
    HashMap totalByState(long orgTreeId, List<String> processStates, HashMap<String, Object> params);

    /**
     * 修改开票系统
     *
     * @param taxInstanceCategoryId
     * @param invoiceSystem
     * @param updateBy
     */
    void updateInvoiceSystem(long taxInstanceCategoryId, String invoiceSystem, long updateBy);

    /**
     * 修改开票类型
     *
     * @param taxInstanceCategoryId
     * @param billType
     * @param updateBy
     */
    void updateBillType(long taxInstanceCategoryId, String billType, long updateBy);


    /**
     * 增值税从Start 改为 Complete 状态,无需申报 使用
     *
     * @param id        税种ID
     * @param processBy 操作人
     * @param type      申报方式  [none yun9 taxOffice handwork undeclare]
     * @param message   日志msg
     */
    void startToComplete(long id, long processBy, DeclareType type, String message);


    /**
     * 税种从Complete 改为 Start 状态。撤销无需申报 使用
     *
     * @param id        税种ID
     * @param processBy 操作人
     * @param type      申报方式  [none yun9 taxOffice handwork undeclare]
     * @param message   日志msg
     */
    void completeToState(long id, long processBy, DeclareType type, String message);


    /**
     * 下载申报扣款截图
     *
     * @param request
     * @param response
     * @param ids      增值税ID
     */
    Object downloadDeclareAndPayImage(HttpServletRequest request, HttpServletResponse response, List<Long> ids);


    /**
     * 根据状态获取分页查询
     *
     * @param orgTreeId 组织ID
     * @param page      页数
     * @param limit     每页条数
     * @param params    过滤参数
     * @return
     */
    Pagination<HashMap> pageByState(List<Long> accountCycleIds, List<TaxSn> taxSns, long orgTreeId, List<Object> processStates, int page, int limit, HashMap<String, Object> params);


    /**
     * 修改银行状态
     *
     * @param bizMdCompanyId
     * @return
     */
    void updateFirstTaxBank(long bizMdCompanyId);


    /**
     * 确认已报
     *
     * @param instanceId
     * @param processBy
     * @param taxSn
     * @param params
     */
    void confirmDeclare(long instanceId, TaxSn taxSn, CycleType cycleType, long processBy, HashMap<String, Object> params);

    /**
     * 撤销确认已报
     *
     * @param id
     * @param processBy
     * @param message
     */
    void cancelDeclare(long id, long processBy, String message);
    void batchCancelAudit(TaxSn taxSn,List<Long> ids,long userId);
    /**
     * 标签统计
     * @param accountCycleIds 会计区间id
     * @param orgTreeId 组织树id
     * @param state 申报状态
     * @param taxSn 税种
     * @param params 参数
     * @return
     */
    List<Map<String,Object>> totalByTaxType(List<Long> accountCycleIds, long orgTreeId, TaxType taxType, TaxSn taxSn, TaxOffice taxOffice, BizTaxInstanceCategory.State state, Map<String, Object> params);
}
