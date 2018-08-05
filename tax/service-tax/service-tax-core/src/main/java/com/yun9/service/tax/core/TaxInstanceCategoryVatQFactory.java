package com.yun9.service.tax.core;


import com.yun9.biz.bill.domain.bo.BizBillInvoiceAgentInvoiceDto;
import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.bill.domain.entity.BizBillInvoiceItem;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryVatSmall;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.dto.BillDTO;
import com.yun9.service.tax.core.dto.BizTaxVatImportDTO;
import com.yun9.service.tax.core.dto.BizTaxVatImportSheetDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:43
 **/
public interface TaxInstanceCategoryVatQFactory {


    /**
     * 根据申报类型进行统计
     *
     * @return
     */
    HashMap totalByDeclareType(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state,Map<String, Object> params);


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
     * 根据ID 获取增值税 累加预缴和申报预缴
     *
     * @param id
     * @return
     */
    ArrayList findById(long id);

    /**
     * 解析excel
     *
     * @param bizTaxVatImportSheetDTO 参数
     */
    Object parseVatExcel(BizTaxVatImportSheetDTO bizTaxVatImportSheetDTO);

    /**
     * 下载excel
     *
     * @param categoryIds      集合
     * @param mdAccountCycleId 会计区间id
     */
    void downloadExcel(HttpServletRequest request, HttpServletResponse response,
                       List<Long> categoryIds, long mdAccountCycleId);


    /**
     * 保存 本期金额
     *
     * @param id
     * @param userId
     */
    void savePrepay(long id, BizTaxInstanceCategoryVatSmall.PrepayTaxSource prepayTaxSource, long userId);
    void savePrepay(long id,  BigDecimal cargoAmount, BigDecimal serviceAmount, long userId);


    void saveBill(List<BillDTO> billDTO, long instanceCategoryVatQId,int state, long userId);

    /**
     * 审核
     *
     * @param id
     * @param userId
     */
    void audit(long id, long userId);

    /**
     * 取消审核
     *
     * @param id
     * @param userId
     */
    void cancelAudit(long id, long userId);

    /**
     *
     * @param companyId
     * @param billType
     * @param bizBillInvoiceAgentInvoiceDto
     */
    void createBill(long id, BizBillInvoice.BillType billType, BizBillInvoiceAgentInvoiceDto bizBillInvoiceAgentInvoiceDto,long userId);


    /**
     *
     * 重置票据
     * @param billDTOs 增值税税种表id
     * @param bizMdAccountCycle
     * @param instanceCategoryVatQId
     * @param userId
     * @param audit 是否自动审核
     */
    void resetBill(List<BillDTO> billDTOs, BizMdAccountCycle bizMdAccountCycle, long instanceCategoryVatQId, long userId, boolean audit);

    /**
     * 修改发票
     * @param billId
     * @param bizBillInvoice
     */
    void updateBill(long id,long billId, BizBillInvoice bizBillInvoice,long userId);
    /**
     * 删除发票(根据发票id单条删除)
     *
     * @param id
     * @return
     */
    void deleteById(long id,long billId,long userId);
    /**
     * 查询应纳税额
     *
     * @param id
     * @return
     */
    Map queryPayTaxAmountById(long id, String type,long userId);
    /**
     * 修改发票明细
     * @param
     * @param bizBillInvoiceItem
     */
    void updateBillItem(long id,long itemId, BizBillInvoiceItem bizBillInvoiceItem,long userId);
}
