package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.yun9.biz.md.*;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.dto.CompanyDataDTO;
import com.yun9.biz.md.domain.dto.InstClientHelper;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInst;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.md.enums.State;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.dto.BizTaxCompanyTaxDTO;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.domain.entity.properties.*;
import com.yun9.biz.tax.enums.*;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.orm.commons.entity.Disabled;
import com.yun9.service.tax.core.TaxInstanceCategoryFactory;
import com.yun9.service.tax.core.enums.TaxLabelEnum;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.utils.OkHttpUtils;
import okhttp3.OkHttpClient;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by zhengzb on 2018/5/7.
 */
@Component
public class TaxInstanceCategoryFactoryImpl implements TaxInstanceCategoryFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryFactoryImpl.class);
    /**
     * 分割数量
     */
    private static final int TOTAL_SIZE = 3;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    BizTaxCompanyTaxService bizTaxCompanyTaxService;

    @Autowired
    BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    BizTaxInstanceCategoryAttachmentService bizTaxInstanceCategoryAttachmentService;

    @Autowired
    BizMdInstService bizMdInstService;

    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;

    @Autowired
    PageCommon pageCommon;

    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;


    @Autowired
    BizTaxCompanyBankService bizTaxCompanyBankService;

    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;
    @Autowired
    BizTaxInstanceCategoryFrService bizTaxInstanceCategoryFrService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    BizTaxInstanceCategoryYhService bizTaxInstanceCategoryYhService;

    @Autowired
    BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;

    @Autowired
    BizTaxInstanceCategoryBitService bizTaxInstanceCategoryBitService;

    @Autowired
    BizTaxMdOfficeCategoryCycleService bizTaxMdOfficeCategoryCycleService;

    @Autowired
    BizTaxMdOfficeService bizTaxMdOfficeService;


    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;


    @Override
    public HashMap totalByException(long orgTreeId, String state, HashMap<String, Object> params) {
        HashMap totleMap = new HashMap();

        //封装请求参数
        List<TaxSn> taxSns = new ArrayList<>();
        List<Long> accountCycleIds = new ArrayList<>();
        if (params != null) {
            if (params.get("taxSns") != null) {
                String[] obj = (String[]) params.get("taxSns");
                for (String key : obj) {
                    taxSns.add(TaxSn.valueOf(key));
                }
                params.put("taxSns", taxSns);
            }
            if (params.get("accountCycleIds") != null) {
                accountCycleIds = Stream.of((Long[]) params.get("accountCycleIds")).collect(Collectors.toList());
                params.put("accountCycleIds", accountCycleIds);
            }
            if (params.get("taxTypes") != null) {
                List<TaxType> taxTypes = new ArrayList();
                String[] obj = (String[]) params.get("taxTypes");
                for (String key : obj) {
                    taxTypes.add(TaxType.valueOf(key));
                }
                params.put("taxTypes", taxTypes);
            }
        }

        Optional.ofNullable(accountCycleIds).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "会计区间不能为空"));

        HashMap<String,Object> instClientMap = bizMdInstOrgTreeClientService.findInstIdAndClientIdByParams(orgTreeId,params);

        List<InstClientHelper> instClientHelpers = (List<InstClientHelper>) instClientMap.get("all");

        List<Long> companyIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());
        List<Long> instClientIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getInstClientId()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(instClientIds)) {
            return totleMap;
        }
        HashMap map;
        if (state == null) {
//            map = bizTaxInstanceCategoryService.totalByEception(null, accountCycleIds, instClientIds, taxSns, params);
            List<Object> processStates = new ArrayList() {{
                add(BizTaxInstanceCategory.ProcessState.exception);
                add(BizTaxInstanceCategory.ProcessState.process);
            }};
            map = bizTaxInstanceService.totalByException(taxSns, accountCycleIds, companyIds,instClientIds, processStates, params);
        } else {
            map = bizTaxInstanceCategoryService.totalByEception(BizTaxInstanceCategory.State.valueOf(state), accountCycleIds, instClientIds, taxSns, params);
        }
        if (map != null) {
            map.forEach((v, k) -> totleMap.put(v, k));
        }
        return totleMap;
    }

    @Override
    public Pagination listLaunch(long orgTreeId, int page, int limit, HashMap<String, Object> params) {

        Pagination pagination = new Pagination();


        //封装数据
        List<TaxSn> taxSns = new ArrayList<>();
        List<Long> accountCycleIds = new ArrayList<>();
        List<CycleType> cycleTypes = new ArrayList<>();
        List<BizTaxMdOfficeCategory.SendType> sendTypes = new ArrayList<>();
        if (params != null) {
            if (params.get("taxSns") != null) {
                String[] obj = (String[]) params.get("taxSns");
                for (String key : obj) {
                    taxSns.add(TaxSn.valueOf(key));
                }
                params.put("taxSns", taxSns);
            }
            if (params.get("cycleTypes") != null) {
                String[] obj = (String[]) params.get("cycleTypes");
                for (String key : obj) {
                    cycleTypes.add(CycleType.valueOf(key));
                }
                params.put("cycleTypes", cycleTypes);
            }
            if (params.get("accountCycleIds") != null) {
                accountCycleIds = Stream.of((Long[]) params.get("accountCycleIds")).collect(Collectors.toList());
                params.put("accountCycleIds", accountCycleIds);
            }
            if (params.get("mdAreaIds") != null) {
                List<Long> mdAreaIds = Stream.of((Long[]) params.get("mdAreaIds")).collect(Collectors.toList());
                params.put("mdAreaIds", mdAreaIds);
            }
            if (params.get("sendTypes") != null) {
                String[] obj = (String[]) params.get("sendTypes");
                for (String key : obj) {
                    sendTypes.add(BizTaxMdOfficeCategory.SendType.valueOf(key));
                }
                params.put("sendTypes", sendTypes);
            }
        }

        Optional.ofNullable(accountCycleIds).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "会计区间不能为空"));
        Optional.ofNullable(taxSns).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "税种不能为空"));

        Map<String, CompanyAccountDTO> companyAccountMap ;
        HashMap<String,Object> instClientMap = bizMdInstOrgTreeClientService.findInstIdAndClientIdByParams(orgTreeId, params);
        List<InstClientHelper> instClientHelpers = (List<InstClientHelper>) instClientMap.get("normal");
        if (CollectionUtils.isEmpty(instClientHelpers)) {
            return pagination.setContent(new ArrayList());
        }

        List<Long> companyIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());
        List<Long> instClientIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getInstClientId()).collect(Collectors.toList());

        //在过滤登陆方式
        if(null != params.get("loginType")) {
            HashMap<String, Object> filterResult = pageCommon.filterLoginType(companyIds, params);
            companyIds = (List<Long>) filterResult.get("companyIds");
            companyAccountMap = (Map<String, CompanyAccountDTO>) filterResult.get("companyAccountMap");
        }else{
            companyAccountMap = pageCommon.getCompanyAccountDTOS(companyIds);
        }

        if(CollectionUtils.isEmpty(companyIds)){
            return pagination.setContent(new ArrayList());
        }

        Pagination<Long> launchIds = bizTaxCompanyCategoryService.findUnreportedInstClients(accountCycleIds, taxSns, companyIds,instClientIds, page, limit, params);

        pagination.setTotalElements(launchIds.getTotalElements());
        pagination.setTotalPages(launchIds.getTotalPages());
        if (CollectionUtils.isEmpty(launchIds.getContent())) {
            pagination.setContent(new ArrayList());
            return pagination;
        }

        List<CompanyDataDTO> companyDataDTOs = bizMdInstClientService.findCompanyDataDTOByCompanyIdsAndOrgTreeId(launchIds.getContent(), orgTreeId);

        Map<String, CompanyAccountDTO> finalCompanyAccountMap = companyAccountMap;
        return pagination.setContent(new ArrayList() {{
            companyDataDTOs.forEach(v -> {
                add(new HashMap() {{
                    put("clientSn", v.getClientSn());
                    put("companyId", v.getCompanyId());
                    put("companyName", v.getCompanyName());
                    put("instClientId", v.getInstClientId());
                    put("state", v.getState());
                    put("taxAreaId", v.getTaxAreaId());
                    put("taxType", v.getTaxType());
                    put("passwordType", new HashMap() {{
                        put(TaxOffice.gs, Optional.ofNullable(finalCompanyAccountMap.get(v.getCompanyId() + "_" + TaxOffice.gs + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                        put(TaxOffice.ds, Optional.ofNullable(finalCompanyAccountMap.get(v.getCompanyId() + "_" + TaxOffice.ds + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                    }});
                }});
            });
        }});
    }

    @Override
    public HashMap totalByState(long orgTreeId, List<String> processStates, HashMap<String, Object> params) {
        HashMap<String, Object> rst = new HashMap() {{
            put("totalStart", 0);
            put("totalError", 0);
            put("mTotalDeduct", 0);
            put("mTotalSend", 0);
            put("mTotalComplete", 0);
            put("qTotalDeduct", 0);
            put("qTotalSend", 0);
            put("qTotalComplete", 0);
        }};
        List<TaxSn> taxSns = new ArrayList<>();
        List<Long> accountCycleIds = new ArrayList<>();
        if (params != null) {
            if (params.get("taxSns") != null) {
                String[] obj = (String[]) params.get("taxSns");
                for (String key : obj) {
                    taxSns.add(TaxSn.valueOf(key));
                }
                params.put("taxSns", taxSns);
            }
            if (params.get("accountCycleIds") != null) {
                accountCycleIds = Stream.of((Long[]) params.get("accountCycleIds")).collect(Collectors.toList());
                params.put("accountCycleIds", accountCycleIds);
            }
            if (params.get("taxTypes") != null) {
                List<TaxType> taxTypes = new ArrayList();
                String[] obj = (String[]) params.get("taxTypes");
                for (String key : obj) {
                    taxTypes.add(TaxType.valueOf(key));
                }
                params.put("taxTypes", taxTypes);
            }
        }

        if (CollectionUtils.isEmpty(taxSns)) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "税种不能为空");
        }
        if (CollectionUtils.isEmpty(accountCycleIds)) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "会计区间不能为空");
        }


        //正常的公司id集合
        HashMap<String,Object> instClientMap = bizMdInstOrgTreeClientService.findInstIdAndClientIdByParams(orgTreeId, params);
        List<InstClientHelper> instClientHelpers = (List<InstClientHelper>) instClientMap.get("all");
        List<InstClientHelper> normalInstClientHelpers = (List<InstClientHelper>) instClientMap.get("normal");

        List<Long> normalCompanyIds = Optional.ofNullable(normalInstClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());
        List<Long> normalInstClientIds = Optional.ofNullable(normalInstClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getInstClientId()).collect(Collectors.toList());
        List<Long> allCompanyIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());
        List<Long> allInstClientIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getInstClientId()).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(normalCompanyIds)) {
            if(null != params.get("loginType")) {
                HashMap<String, Object> filterResult = pageCommon.filterLoginType(normalCompanyIds, params);
                normalCompanyIds = (List<Long>) filterResult.get("companyIds");
            }
            //发起申报总数
            rst.put("totalStart", bizTaxCompanyCategoryService.countUnreportedInstClients(accountCycleIds, taxSns, normalCompanyIds,normalInstClientIds, params));
        }

        if (CollectionUtils.isNotEmpty(allCompanyIds)) {
            if(null != params.get("loginType")) {
                HashMap<String, Object> filterResult = pageCommon.filterLoginType(allCompanyIds, params);
                allCompanyIds = (List<Long>) filterResult.get("companyIds");
            }
            //发起错误
            rst.put("totalError", bizTaxInstanceService.totalByState(taxSns, accountCycleIds, allCompanyIds,allInstClientIds, processStates, params));
        }

        if (CollectionUtils.isEmpty(allInstClientIds)) {
            return rst;
        }

        //发起处理过程中的各种状态客户数量统计
        HashMap<String, Object> totalMap = bizTaxInstanceCategoryService.totalByAllStateGroup(accountCycleIds, allInstClientIds, taxSns, params);
        if (totalMap != null) {
            rst.put("mTotalDeduct", totalMap.get("m_deduct") == null ? 0 : totalMap.get("m_deduct"));
            rst.put("mTotalSend", totalMap.get("m_send") == null ? 0 : totalMap.get("m_send"));
            rst.put("mTotalComplete", totalMap.get("m_complete") == null ? 0 : totalMap.get("m_complete"));
            rst.put("qTotalDeduct", totalMap.get("q_deduct") == null ? 0 : totalMap.get("q_deduct"));
            rst.put("qTotalSend", totalMap.get("q_send") == null ? 0 : totalMap.get("q_send"));
            rst.put("qTotalComplete", totalMap.get("q_complete") == null ? 0 : totalMap.get("q_complete"));
        }
        return rst;
    }


    @Override
    public void updateInvoiceSystem(long taxInstanceCategoryId, String invoiceSystem, long updateBy) {
        if (!InvoiceSystem.isInvoiceSystem(invoiceSystem)) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "不存在改开票系统");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(taxInstanceCategoryId);
        Optional.ofNullable(bizTaxInstanceCategory).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.TaxInstanceCategoryNotFound));
        BizTaxInstance bizTaxInstance = bizTaxInstanceCategory.getBizTaxInstance();
        BizTaxCompanyTaxDTO companyTaxDTO = new BizTaxCompanyTaxDTO();
        companyTaxDTO.setInvoiceSystem(InvoiceSystem.valueOf(invoiceSystem));
        bizTaxCompanyTaxService.update(bizTaxInstance.getMdCompanyId(), companyTaxDTO);
    }

    @Override
    public void updateBillType(long taxInstanceCategoryId, String billType, long updateBy) {
        if (!BillingType.isBillingType(billType)) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "不存在该开票类型");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(taxInstanceCategoryId);
        Optional.ofNullable(bizTaxInstanceCategory).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.TaxInstanceCategoryNotFound));
        BizTaxInstance bizTaxInstance = bizTaxInstanceCategory.getBizTaxInstance();
        BizTaxCompanyTaxDTO companyTaxDTO = new BizTaxCompanyTaxDTO();
        companyTaxDTO.setBillingType(BillingType.valueOf(billType));
        bizTaxCompanyTaxService.update(bizTaxInstance.getMdCompanyId(), companyTaxDTO);
    }

    /**
     * 无需申报
     *
     * @param id        税种ID
     * @param processBy 操作人
     * @param type      申报方式  [none yun9 taxOffice handwork undeclare]
     * @param message   日志msg
     */
    @Override
    public void startToComplete(long id, long processBy, DeclareType type, String message) {
        //检查税种
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(id))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,税种实例不存在"));


        //检查未申报
        if (bizTaxInstanceCategory.getDeclareType() != DeclareType.none) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败，申报方式不是\"未申报\"状态,不能进行该操作");
        }

        //检查税种状态为已启用
        if (!bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.send)) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前状态不是\"申报\"状态");
        }

        //检查税种状态为已启用
        if (type == DeclareType.handwork) {//确认已报
            //税种状态必须是一件启用和none
            if (bizTaxInstanceCategory.getTaxOfficeConfirm() == BizTaxInstanceCategory.TaxOfficeConfirm.disabled) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,税种状态不是\"已启用\" 和 \"none\"");
            }
        } else if (type == DeclareType.undeclare) { //无需申报
            //检查客户状态
            BizMdInstClient bizMdInstClient = Optional.ofNullable(bizMdInstClientService.findById(bizTaxInstanceCategory.getBizTaxInstance().getMdInstClientId()))
                    .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "操作失败，客户状态信息不存在"));

            if (bizTaxInstanceCategory.getTaxOfficeConfirm() != BizTaxInstanceCategory.TaxOfficeConfirm.disabled && bizMdInstClient.getState() != State.disabled) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,客户状态不是已停止服务,并且税种状态不是\"未启用\"");
            }
        }

        //检查执行状态
        bizTaxInstanceCategoryService.isProcess(bizTaxInstanceCategory); //当前code是否允许继续

        //修改状态
        bizTaxInstanceCategoryService.updateStartToCompleteByDeclareType(bizTaxInstanceCategory.getId(), processBy, type, message);

    }

    /**
     * 撤销无需申报
     *
     * @param id        税种ID
     * @param processBy 操作人
     * @param type      申报方式  [none yun9 taxOffice handwork undeclare]
     * @param message   日志msg
     */
    @Override
    public void completeToState(long id, long processBy, DeclareType type, String message) {


        //检查税种状态是否为Complete
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(id))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,税种实例不存在"));

        //判断State 条件
        if (!bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.complete)) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前状态不是已完成");
        }

        //判断ProcessState 状态
        if (!bizTaxInstanceCategory.getProcessState().equals(BizTaxInstanceCategory.ProcessState.success)) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前执行状态不是\"成功\"");
        }

        //检查申报方式
        BizTaxInstanceCategory.State state = BizTaxInstanceCategory.State.send;
        if (bizTaxInstanceCategory.getDeclareType() != DeclareType.undeclare) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前申报方式不是\"无需申报\"");
        }


        //检查执行状态
        bizTaxInstanceCategoryService.isProcess(bizTaxInstanceCategory); //当前code是否允许继续

        //修改状态
        bizTaxInstanceCategoryService.updateCompleteToState(bizTaxInstanceCategory.getId(), processBy, state, message);

    }


    @Override
    public Object downloadDeclareAndPayImage(HttpServletRequest request, HttpServletResponse response, List<Long> ids) {

        //获取税种
        final List<BizTaxInstanceCategory> bizTaxInstanceCategorys = Optional.ofNullable(bizTaxInstanceCategoryService.findByIdIn(ids))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,税种实例不存在"));

        if (bizTaxInstanceCategorys.size() == 0) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,税种实例不存在");
        }

        final BizTaxInstance bizTaxInstance = bizTaxInstanceCategorys.get(0).getBizTaxInstance();


        //获取机构客户
        final BizMdInstClient bizMdInstClient = Optional.ofNullable(bizMdInstClientService.findById(bizTaxInstance.getMdInstClientId()))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,机构客户不存在"));

        //获取机构名字
        BizMdInst bizMdInst = Optional.ofNullable(bizMdInstService.findById(bizMdInstClient.getBizMdInstId()))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,机构不存在"));

        //获取会计区间
        BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(bizMdAccountCycleService.findById(bizTaxInstance.getMdAccountCycleId()))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,会计区间不存在"));
        ;


        Map<Long, BizTaxInstanceCategory> bizTaxInstanceCategoryMaps = new HashMap<Long, BizTaxInstanceCategory>() {{
            bizTaxInstanceCategorys.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};

        //获取税类型
        final List<BizTaxMdCategory> bizTaxMdCategorys = Optional.ofNullable(bizTaxMdCategoryService.findAll())
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "税种配置数据不存在"));

        Map<Long, BizTaxMdCategory> BizTaxMdCategoryMaps = new HashMap<Long, BizTaxMdCategory>() {{
            bizTaxMdCategorys.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};

        //获取附件
        final List<BizTaxInstanceCategoryAttachment> attachments = Optional.ofNullable(bizTaxInstanceCategoryAttachmentService.findByBizTaxInstanceCategoryIdIn(ids))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "没有申报截图和纳税凭证"));
        if (attachments.size() == 0) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "没有申报截图和纳税凭证");
        }

        String downloadFilename = bizMdInst.getName() + ".zip";//文件的名称

        //返回客户端浏览器的版本号、类型，针对IE或者以IE为内核的浏览器：
        String userAgent = request.getHeader("User-Agent");
        try {
            if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
                downloadFilename = java.net.URLEncoder.encode(downloadFilename, "UTF-8");
            } else {
                downloadFilename = new String(downloadFilename.getBytes("UTF-8"), "ISO-8859-1");//非IE
            }
        } catch (UnsupportedEncodingException e) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "下载图片失败失败，请重新下载。失败原因:" + e.getMessage());
        }
        response.setContentType("application/x-download");//告知浏览器下载文件，而不是直接打开，浏览器默认为打开
        response.setHeader("Content-Disposition", "attachment;fileName=\"" + downloadFilename + "\"");

        //设置代理
//        OkHttpClient okHttpClient = OkHttpUtils.getClient();
        String host = "172.31.100.157";
        String port = "38080";
        System.setProperty("proxySet", "true");
        System.setProperty("proxyHost", host);
        System.setProperty("proxyPort", port);

        
        //开始下载图片
        try {

            ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
            zos.setMethod(ZipOutputStream.DEFLATED); //设置压缩方法
            zos.setEncoding("gbk");

            attachments.forEach(e -> {
                if (e != null && e.getUrl() != null && e.getUrl().length() > 0) {
                    URL url = null;
                    try {
                        url = new URL(e.getUrl());
                        String fileType = e.getUrl().substring(e.getUrl().lastIndexOf("."));
                        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryMaps.get(e.getBizTaxInstanceCategoryId());
                        zos.putNextEntry(new ZipEntry(bizMdInst.getName() + "/"
                                + bizMdAccountCycle.getName() + "/"
                                + BizTaxMdCategoryMaps.get(bizTaxInstanceCategory.getBizTaxMdCategoryId()).getSimpleName() + "/"
                                + (e.getType() == BizTaxInstanceCategoryAttachment.Type.declare ? "申报截图" : "纳税凭证") + "/"
                                + bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName()
                                + e.getId()
                                + fileType) //文件类型
                        );

                        InputStream fis = url.openConnection().getInputStream();
                        byte[] buffer = new byte[1024];
                        int r = 0;
                        while ((r = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, r);
                        }

                        zos.closeEntry();
                        fis.close();

                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                        throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "下载图片失败失败，请重新下载。失败原因:" + e1.getMessage());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "下载图片失败失败，请重新下载。失败原因:" + e1.getMessage());
                    }
                }
            });

            zos.flush();
            zos.close();
            return null;//解决 getOutputStream() has already been called for this response
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "下载图片失败失败，请重新下载。失败原因:" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "下载图片失败失败，请重新下载。失败原因:" + e.getMessage());
        }
    }


    @Override
    public Pagination<HashMap> pageByState(List<Long> accountCycleIds, List<TaxSn> taxSns, long orgTreeId, List<Object> processStates, int page, int limit, HashMap<String, Object> params) {

        Pagination pagination = new Pagination();
        pagination.setContent(new ArrayList());

        //1 检查组织id
        //正常的公司id集合
        HashMap<String,Object> instClientMap = bizMdInstOrgTreeClientService.findInstIdAndClientIdByParams(orgTreeId, null);
        List<InstClientHelper> instClientHelpers = (List<InstClientHelper>) instClientMap.get("all");
        List<Long> companyIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());
        List<Long> instClientIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getInstClientId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(instClientIds)) {
            return pagination;
        }


        //查询过滤税种id
        List<Long> officeCategoryIds = bizTaxMdOfficeCategoryService.findAllByTaxSnAndParams(taxSns, params);
        if (CollectionUtils.isEmpty(officeCategoryIds)) {
            return pagination;
        }


        //求并集
        List<BizTaxCompanyCategory> bizTaxCompanyCategorys = bizTaxCompanyCategoryService.findByMdCompanyIdsAndBizTaxMdOfficeCategoryIds(companyIds, officeCategoryIds);
        if (CollectionUtils.isNotEmpty(bizTaxCompanyCategorys)) {
            companyIds = bizTaxCompanyCategorys.stream().map(v -> v.getBizMdCompanyId()).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(companyIds)) {
                return pagination;
            }
        }



        Pagination<BizTaxInstance> pageObj = bizTaxInstanceService.pageByState(accountCycleIds, companyIds,instClientIds, processStates, page, limit, params);
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());


        //获取passwordType
        companyIds = pageObj.getContent().stream().map(v -> v.getMdCompanyId()).collect(Collectors.toList());
        Map<String, CompanyAccountDTO> companyAccountMap = pageCommon.getCompanyAccountDTOS(companyIds);

        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{
                    put("passwordType", new HashMap() {{
                        put(TaxOffice.gs, Optional.ofNullable(companyAccountMap.get(v.getMdCompanyId() + "_" + TaxOffice.gs + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                        put(TaxOffice.ds, Optional.ofNullable(companyAccountMap.get(v.getMdCompanyId() + "_" + TaxOffice.ds + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                    }});

                    put("id", v.getId()); //纳税申报ID
                    put("companyId", v.getMdCompanyId());
                    put("clientSn", v.getMdClientSn()); //编号
                    put("companyName", v.getMdCompanyName());//公司名字
                    put("taxAreaId", v.getMdAreaId());//税区
                    put("taxType", v.getMdCompanyTaxType());//纳税方式 小规模、个体
                    put("taxOffice", v.getTaxOffice());//税局
                    put("processCodeId", v.getProcessCodeId());//办理ID
                    put("processState", v.getProcessState());//办理状态
                    put("processMessage", v.getProcessMessage());//办理说明
                }});
            });
        }});
        return pagination;

    }


    @Override
    public void updateFirstTaxBank(long id) {
        //1 根据ID获取扣款银行对象 {biz_tax_company_bank} {id}，对象不存在，则提示"参数错误，该纳税扣款绑定银行不存在"
        BizTaxCompanyBank bizTaxCompanyBank = Optional.ofNullable(bizTaxCompanyBankService.findById(id))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "该纳税扣款绑定银行不存在"));

        //2 根据对象{biz_md_company_id}，修改改公司下所有的绑定银行{is_first_tax}为非首选缴税（修改值为0）
        bizTaxCompanyBankService.updateTaxCompanyBanksByBizMdCompanyId(bizTaxCompanyBank.getBizMdCompanyId(), 0);

        //3 修改当前扣款银行对象为{is_first_tax}为首选缴税（修改值为1）
        bizTaxCompanyBank.setIsFirstTax(1);
        bizTaxCompanyBankService.create(bizTaxCompanyBank);
    }

    /**
     * 手动申报
     *
     * @param instanceId
     * @param processBy
     * @param taxSn
     * @param params
     */
    @Override
    public void confirmDeclare(long instanceId, TaxSn taxSn, CycleType cycleType, long processBy, HashMap<String, Object> params) {
        //获取实例
        BizTaxInstance bizTaxInstance = Optional.ofNullable(bizTaxInstanceService.findById(instanceId))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "申报实例不存在"));
        //组装数据
        BizTaxMdCategory bizTaxMdCategory = Optional.ofNullable(bizTaxMdCategoryService.findBySn(taxSn))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "税种不存在"));


        //======== 查找申报期间 ========
        //1.更加地区编号 和 税局 找到税种支持区域
        BizTaxMdOffice bizTaxMdOffice = Optional.ofNullable(bizTaxMdOfficeService.findByAreaIdAndSn(bizTaxInstance.getMdAreaId(), bizTaxInstance.getTaxOffice()))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "无法获取税种申报所属期"));

        //2.根据支持区域编号和税种id和申报周期id获取 税种区域
        List<BizTaxMdOfficeCategory> bizTaxMdOfficeCategorys = bizTaxMdOfficeCategoryService.findByBizTaxMdOfficeIdAndBizTaxMdCategoryIdAndCycleType(bizTaxMdOffice.getId(), bizTaxMdCategory.getId(), cycleType);
        if (CollectionUtils.isEmpty(bizTaxMdOfficeCategorys) || bizTaxMdOfficeCategorys.size() == 0) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "无法获取税种申报所属期");
        }

        //3.获取税种申报周期
        BizTaxMdOfficeCategoryCycle bizTaxMdOfficeCategoryCycle = bizTaxMdOfficeCategoryCycleService.findBizTaxMdOfficeCategoryId(bizTaxMdOfficeCategorys.get(0).getId(), bizTaxInstance.getMdAccountCycleId());


        //======== 看是否创建子税种 ========
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findByBizTaxMdCategoryIdAndBizTaxInstanceId(bizTaxMdCategory.getId(), bizTaxInstance.getId());


        bizTaxInstance.setProcessCodeId(null);
        bizTaxInstance.setProcessMessage(null);
        bizTaxInstance.setProcessState(BizTaxInstanceCategory.ProcessState.success);
        bizTaxInstanceService.create(bizTaxInstance);

        if (bizTaxInstanceCategory == null) {
            //构建税种已经子税种
            bizTaxInstanceCategory = pageCommon.createTax(bizTaxInstance, bizTaxMdCategory, bizTaxMdOfficeCategoryCycle, cycleType, taxSn, params);

        } else {
            //修改状态
            bizTaxInstanceCategory.setStartDate(bizTaxMdOfficeCategoryCycle.getBeginDate());
            bizTaxInstanceCategory.setCloseDate(bizTaxMdOfficeCategoryCycle.getEndDate());
            bizTaxInstanceCategory.setDeclareType(DeclareType.handwork);
            bizTaxInstanceCategory.setProcessState(BizTaxInstanceCategory.ProcessState.success);
            bizTaxInstanceCategory.setState(BizTaxInstanceCategory.State.complete);
            bizTaxInstanceCategory.setUpdatedAt(System.currentTimeMillis()/1000L);
            bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);
        }

        //写日志
        bizTaxInstanceCategoryHistoryService.log(bizTaxInstanceCategory.getId(), processBy, BizTaxInstanceCategoryHistory.Type.handwork, params.get("remark").toString());

    }

    /**
     * 撤销手动申报
     *
     * @param id
     * @param processBy
     * @param message
     */
    @Override
    public void cancelDeclare(long id, long processBy, String message) {

        //检查税种状态是否为Complete
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(id))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,税种实例不存在"));


        //获取实例
        BizTaxInstance bizTaxInstance = Optional.ofNullable(bizTaxInstanceService.findById(bizTaxInstanceCategory.getBizTaxInstanceId()))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "申报实例不存在"));


        //判断State 条件
        if (!bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.complete)) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前状态不是已完成");
        }

        //判断ProcessState 状态
        if (!bizTaxInstanceCategory.getProcessState().equals(BizTaxInstanceCategory.ProcessState.success)) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前执行状态不是\"成功\"");
        }

        //检查申报方式
        if (bizTaxInstanceCategory.getDeclareType() != DeclareType.handwork) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败,当前申报方式不是\"确认已报\"");
        }

        //检查执行状态
        bizTaxInstanceCategoryService.isProcess(bizTaxInstanceCategory); //当前code是否允许继续

        //修改状态
        bizTaxInstanceCategory.setState(BizTaxInstanceCategory.State.send);
        bizTaxInstanceCategory.setProcessCodeId(BizTaxMdMsgCode.Process.cancel_need_redownload_data.getCode());
        bizTaxInstanceCategory.setProcessState(BizTaxInstanceCategory.ProcessState.exception);
        bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);

        bizTaxInstance.setProcessState(BizTaxInstanceCategory.ProcessState.none);
        bizTaxInstance.setProcessMessage(null);
        bizTaxInstanceService.create(bizTaxInstance);

        bizTaxInstanceCategoryHistoryService.log(bizTaxInstanceCategory.getId(), processBy, BizTaxInstanceCategoryHistory.Type.complete, message);
    }

    @Override
    public void batchCancelAudit(TaxSn taxSn, List<Long> ids, long userId) {
        logger.debug("开始批量撤销审核");
        switch (taxSn) {
            case m_fz:
            case q_fz:
                logger.debug("开始撤销审核附征税");
                for (long id : ids) {
                    BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = Optional.ofNullable(
                            bizTaxInstanceCategoryFzService.findById(id)
                    ).orElseThrow(() ->
                            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到附征税申报实例")
                    );
                    //判断可用
                    if (!bizTaxInstanceCategoryFz.isEnable()) {
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
                    }
                    BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryFz.getBizTaxInstanceCategory();
                    if (!bizTaxInstanceCategory.isAudit()) {
                        BizTaxException
                                .throwException(BizTaxException.Codes.BizTaxException, "税种未审核,无需撤销审核");
                    }
                    //统一撤销
                    taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);

                }
                logger.debug("批量撤销审核附征税完成");
                break;
            case m_yh:
                logger.debug("开始撤销审核印花税");
                for (long id : ids) {
                    BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = bizTaxInstanceCategoryYhService.findById(id);
                    if (null == bizTaxInstanceCategoryYh) {
                        BizTaxException
                                .throwException(BizTaxException.Codes.BizTaxException, "税种实例为空!");
                    }
                    //判断可用
                    if (!bizTaxInstanceCategoryYh.isEnable()) {
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "印花税申报实例不可用");
                    }
                    BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryYh.getBizTaxInstanceCategory();
                    if (!bizTaxInstanceCategory.isAudit()) {
                        BizTaxException
                                .throwException(BizTaxException.Codes.BizTaxException, "税种未审核,无需撤销审核");
                    }
                    //统一撤销
                    taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);

                }
                logger.debug("批量撤销审核印花税完成");
                break;
            case q_bit:
                logger.debug("开始撤销审核企业所得税");
                for (long id : ids) {
                    BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = bizTaxInstanceCategoryBitService.findById(id);
                    if (null == bizTaxInstanceCategoryBit) {
                        BizTaxException
                                .throwException(BizTaxException.Codes.BizTaxException, "税种实例为空!");
                    }
                    //判断可用
                    if (!bizTaxInstanceCategoryBit.isEnable()) {
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "印花税申报实例不可用");
                    }
                    BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryBit.getBizTaxInstanceCategory();
                    if (!bizTaxInstanceCategory.isAudit()) {
                        BizTaxException
                                .throwException(BizTaxException.Codes.BizTaxException, "税种未审核,无需撤销审核");
                    }
                    //统一撤销
                    taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);

                }
                logger.debug("批量撤销审核企业所得税完成");
                break;
            default:
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "暂不支持当前税种" + taxSn + "的批量撤销审核！");
        }
    }


    @Override
    public List<Map<String, Object>> totalByTaxType(List<Long> accountCycleIds, long orgTreeId, TaxType taxType,TaxSn taxSn, TaxOffice taxOffice, BizTaxInstanceCategory.State state, Map<String, Object> params) {
        List<Map<String, Object>> allLabel = new ArrayList<>();
        logger.debug("开始统计标签列表");
        //根据组织ID获取 机构客户ID
        List<Long> instClientIds = bizMdInstOrgTreeClientService.findInstClientIdsByOrgTreeId(orgTreeId);
        if (CollectionUtils.isEmpty(instClientIds)) {
            return allLabel;
        }
        //获取所有的实例
//        List<BizTaxInstance> bizTaxInstances = bizTaxInstanceService.findByCompanys(instClientIds, accountCycleIds, taxOffice);
//        if (CollectionUtils.isEmpty(bizTaxInstances)) {
//            return allLabel;
//        }
//        List<BizTaxInstance> bizTaxInstances = new ArrayList<>();
//        List<List<Long>> accountCycleIdList = Lists.partition(accountCycleIds, TOTAL_SIZE);
//        if (CollectionUtils.isEmpty(accountCycleIdList)) {
//            return allLabel;
//        }
//        for (List<Long> list : accountCycleIdList) {
//            bizTaxInstances.addAll(bizTaxInstanceService.totalByTaxType(list, instClientIds,taxOffice, params));
//        }

        //得到税种
        BizTaxMdCategory bizTaxMdCategory = Optional.ofNullable(bizTaxMdCategoryService.findBySn(taxSn))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "税种不存在"));

        String companyName = StringUtils.isEmpty(params.get("companyName")) ? "":params.get("companyName").toString() ;
        String clientSn = StringUtils.isEmpty(params.get("clientSn")) ? "":params.get("clientSn").toString() ;
        logger.debug("开始查询====税种参数{},,,{},,,,{},,,,,{},,,,{},,,{},,,{},,,", taxOffice, accountCycleIds, instClientIds, state, bizTaxMdCategory.getId(), companyName, clientSn);
        List<BizLabel> bizTaxInstances = bizTaxInstanceCategoryService.findByBizLabel(taxOffice, accountCycleIds.stream().toArray(Long[]::new), instClientIds.stream().toArray(Long[]::new), state.name(), bizTaxMdCategory.getId(), companyName, clientSn);
        logger.debug("查询结果{}", bizTaxInstances);
        if (CollectionUtils.isEmpty(bizTaxInstances)) {
            return allLabel;
        }
        if (TaxSn.q_vat.equals(taxSn) && TaxType.small.equals(taxType)){
            bizTaxInstances = bizTaxInstances.stream().filter(k->TaxType.small.equals(k.getMdCompanyTaxType()) || TaxType.personal.equals(k.getMdCompanyTaxType()) ).collect(Collectors.toList());
        }
        if (TaxSn.q_vat.equals(taxSn) && TaxType.normal.equals(taxType)){
            bizTaxInstances = bizTaxInstances.stream().filter(k->TaxType.normal.equals(k.getMdCompanyTaxType())).collect(Collectors.toList());
        }
        if (TaxSn.q_fz.equals(taxSn)){
            bizTaxInstances = bizTaxInstances.stream().filter(k->TaxType.small.equals(k.getMdCompanyTaxType()) || TaxType.personal.equals(k.getMdCompanyTaxType()) ).collect(Collectors.toList());
        }
        if (TaxSn.m_fz.equals(taxSn)){
            bizTaxInstances = bizTaxInstances.stream().filter(k->TaxType.normal.equals(k.getMdCompanyTaxType())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(bizTaxInstances)) {
            return allLabel;
        }

//        List<Long> instances = bizTaxInstances.stream().map(BizTaxInstance::getId).collect(Collectors.toList());
//        List<BizTaxInstanceCategory> bizTaxInstanceCategories = bizTaxInstanceCategoryService.findByBizTaxInstanceIdInAndStateAndBizTaxMdCategoryId(instances, state, bizTaxMdCategory.getId());
        logger.debug("税种集合:{}", bizTaxInstances);




        //公用的统计缴税账户状态  state 缴税
        if (BizTaxInstanceCategory.State.deduct.equals(state)  && CollectionUtils.isNotEmpty(  bizTaxInstances)) {
//            List<Long> companyIds = bizTaxInstanceCategories.stream().map(BizTaxInstanceCategory::getBizTaxInstance).map(BizTaxInstance::getMdCompanyId).distinct().collect(Collectors.toList());
//            List<BizTaxInstance> listInstance = bizTaxInstanceCategories.stream().map(BizTaxInstanceCategory::getBizTaxInstance).collect(Collectors.toList());
                List<Long> companyIds = bizTaxInstances.stream().map(BizLabel::getMdCompanyId).distinct().collect(Collectors.toList());
                logger.debug("缴税账户状态_公司总数ids:{}", companyIds);
                this.totalTaxAccount(allLabel, companyIds, taxOffice);


        }

        //申报
        if (BizTaxInstanceCategory.State.send.equals(state) && CollectionUtils.isNotEmpty(bizTaxInstances)) {
            //税额状态 企业所得税 工资薪金个税 生产经营个税 印花税 附征税
            if (TaxSn.q_bit.equals(taxSn) || TaxSn.m_bit.equals(taxSn)
                    || TaxSn.m_personal_payroll.equals(taxSn)
                    || TaxSn.m_personal_business.equals(taxSn) || TaxSn.q_personal_business.equals(taxSn)
                    || TaxSn.m_yh.equals(taxSn)
                    || TaxSn.q_fz.equals(taxSn) || TaxSn.m_fz.equals(taxSn)) {
                this.totalPayTaxDeclareType(allLabel, bizTaxInstances);
            }
            //报表状态 工资薪金个税 印花税 付征税
            if (TaxSn.m_personal_payroll.equals(taxSn)
                    || TaxSn.m_yh.equals(taxSn)
                    || TaxSn.q_fz.equals(taxSn) || TaxSn.m_fz.equals(taxSn)) {
                this.totalPayReprotAudit(allLabel, bizTaxInstances);
            }
            //审核状态 增值税 企业所得税 生产经营个税
            if (TaxSn.q_vat.equals(taxSn) || TaxSn.m_vat.equals(taxSn)
                    || TaxSn.q_bit.equals(taxSn) || TaxSn.m_bit.equals(taxSn)
                    || TaxSn.m_personal_business.equals(taxSn) || TaxSn.q_personal_business.equals(taxSn)) {
                this.totalPayTaxAudit(allLabel, bizTaxInstances);
            }
            //财务报表备案
            if (TaxSn.q_fr.equals(taxSn)) {
                this.totalPayTaxRecord(allLabel, bizTaxInstances);
            }
            //工资薪金个税 申报来源
            if (TaxSn.m_personal_payroll.equals(taxSn)) {
                this.totalPaySourceType(allLabel, bizTaxInstances);
            }
            //印花税  增值税状态
            if (TaxSn.m_yh.equals(taxSn)) {
                this.totalPayFhVatStateType(allLabel, bizTaxInstances);
            }
            //附征税 申报
            if (TaxSn.q_fz.equals(taxSn) || TaxSn.m_fz.equals(taxSn)) {
                this.totalPayFzDirectTaxStateType(allLabel, bizTaxInstances, taxSn, state);
            }
            if  (TaxSn.q_vat.equals(taxSn)){
                this.totalPayVatTaxDeclareType(allLabel,bizTaxInstances);
            }
        }

        //完成
        if (BizTaxInstanceCategory.State.complete.equals(state) && CollectionUtils.isNotEmpty(bizTaxInstances)) {
            //列表申报方式
            this.totalPayDeclareType(allLabel, bizTaxInstances);
            //税额状态 企业所得税 工资薪金个税 生产经营个税 印花税 附征税
            if (TaxSn.q_bit.equals(taxSn) || TaxSn.m_bit.equals(taxSn)
                    || TaxSn.m_personal_payroll.equals(taxSn)
                    || TaxSn.m_personal_business.equals(taxSn) || TaxSn.q_personal_business.equals(taxSn)
                    || TaxSn.m_yh.equals(taxSn)
                    || TaxSn.q_fz.equals(taxSn) || TaxSn.m_fz.equals(taxSn)) {
                this.totalPayTaxDeclareType(allLabel, bizTaxInstances);
            }
            //财务报表备案
            if (TaxSn.q_fr.equals(taxSn)) {
                this.totalPayTaxRecord(allLabel, bizTaxInstances);
            }
            if  (TaxSn.q_vat.equals(taxSn)){
                this.totalPayVatTaxDeclareType(allLabel,bizTaxInstances);
            }
        }
//        //停止服务
        if (BizTaxInstanceCategory.State.send.equals(state) || BizTaxInstanceCategory.State.deduct.equals(state) || BizTaxInstanceCategory.State.complete.equals(state)) {
            List<Long> instClientStateTypeIds = bizTaxInstances.stream().map(BizLabel::getInstClient).distinct().collect(Collectors.toList());
            System.out.println("==============================================+" + instClientStateTypeIds);
            List<BizMdInstClient> bizMdInstClients = bizMdInstClientService.findByIds(instClientStateTypeIds);
            logger.debug("检查停止服务{}", instClientStateTypeIds);
            if (CollectionUtils.isNotEmpty(bizMdInstClients)) {
                //停止服务
                long disabledVounts = bizMdInstClients.stream().filter(k -> State.disabled.equals(k.getState())).distinct().count();
                if (disabledVounts > 0) {
                    allLabel.add(new HashMap<String, Object>(7) {{
                        put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.InstClientStateType.VAT_SALE_AMOUNT.getDefSn());
                        put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.InstClientStateType.VAT_SALE_AMOUNT.getSn());
                        put(TaxLabelEnum.Lable.count.toString(), disabledVounts);
                    }});
                }
            }
        }

        return allLabel;
    }

    /**
     * 缴税账户状态小计
     *
     * @param allLabel   标签集合
     * @param companyIds 公司集合
     * @param taxOffice  国地税
     */
    private void totalTaxAccount(List<Map<String, Object>> allLabel, List<Long> companyIds, TaxOffice taxOffice) {
        List<BizTaxCompanyBank> bizTaxCompanyBanks = bizTaxCompanyBankService.findByAndBizMdCompanyIdInAndTaxOffice(companyIds, taxOffice);
        if (CollectionUtils.isNotEmpty(bizTaxCompanyBanks)) {
            //得到有账户的总数
            long noCounts = bizTaxCompanyBanks.stream().map(BizTaxCompanyBank::getBizMdCompanyId).distinct().count();
            if (companyIds.size() != noCounts) {
                //统计无缴税账户的数量
                logger.debug("缴税账户状态_无缴税账户ids:{}", companyIds.size() - noCounts);
                if (companyIds.size() - noCounts > 0) {
                    allLabel.add(new HashMap<String, Object>(7) {{
                        put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.CompanyBankStateEnum.NO_TAX_ACCOUNT.getDefSn());
                        put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.CompanyBankStateEnum.NO_TAX_ACCOUNT.getSn());
                        put(TaxLabelEnum.Lable.count.toString(), companyIds.size() - noCounts);
                    }});
                }
            }
            //得到确认账户  判断是否首选缴税为1
            long counts = bizTaxCompanyBanks.stream().filter(k -> k.getIsFirstTax() == Disabled.DISABLE.getValue()).map(BizTaxCompanyBank::getBizMdCompanyId).distinct().count();
            logger.debug("缴税账户状态_缴税账户ids:{}", counts);
            if (noCounts - counts > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.CompanyBankStateEnum.UN_TAX_ACCOUNT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.CompanyBankStateEnum.UN_TAX_ACCOUNT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), companyIds.size() - counts);
                }});
            }
            if (counts > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.CompanyBankStateEnum.TAX_ACCOUNT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.CompanyBankStateEnum.TAX_ACCOUNT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), counts);
                }});
            }
        } else {
            //统计无缴税账户的数量
            logger.debug("缴税账户状态_无缴税账户ids:{}", companyIds);
            if (companyIds.size() > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.CompanyBankStateEnum.NO_TAX_ACCOUNT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.CompanyBankStateEnum.NO_TAX_ACCOUNT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), companyIds.size());
                }});
            }
        }
    }

    /**
     * 列表税额小计状态
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayTaxDeclareType(List<Map<String, Object>> allLabel,  List<BizLabel> bizTaxInstanceCategories) {
        long taxableCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.taxable.equals(k.getTaxDeclareType())).count();
        if (taxableCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.TaxDeclareType.TAX_ABLE.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.TaxDeclareType.TAX_ABLE.getSn());
                put(TaxLabelEnum.Lable.count.toString(), taxableCount);
            }});
        }
        long taxfreeCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.taxfree.equals(k.getTaxDeclareType())).count();
        if (taxfreeCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.TaxDeclareType.TAX_FREE.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.TaxDeclareType.TAX_FREE.getSn());
                put(TaxLabelEnum.Lable.count.toString(), taxfreeCount);
            }});
        }
    }

    /**
     * 申报状态
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayVatTaxDeclareType(List<Map<String, Object>> allLabel,  List<BizLabel> bizTaxInstanceCategories) {
        long zeroCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.zero.equals(k.getTaxDeclareType())).count();
        if (zeroCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.VatTaxDeclareType.ZERO.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.VatTaxDeclareType.ZERO.getSn());
                put(TaxLabelEnum.Lable.count.toString(), zeroCount);
            }});
        }
        long drawbackCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.drawback.equals(k.getTaxDeclareType())).count();
        if (drawbackCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.VatTaxDeclareType.DRAWBACK.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.VatTaxDeclareType.DRAWBACK.getSn());
                put(TaxLabelEnum.Lable.count.toString(), drawbackCount);
            }});
        }
        long taxableCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.taxable.equals(k.getTaxDeclareType())).count();
        if (taxableCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.VatTaxDeclareType.TAX_ABLE.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.VatTaxDeclareType.TAX_ABLE.getSn());
                put(TaxLabelEnum.Lable.count.toString(), taxableCount);
            }});
        }
        long taxfreeCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.taxfree.equals(k.getTaxDeclareType())).count();
        if (taxableCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.VatTaxDeclareType.TAX_FREE.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.VatTaxDeclareType.TAX_FREE.getSn());
                put(TaxLabelEnum.Lable.count.toString(), taxfreeCount);
            }});
        }
    }

    /**
     * 列表报表确认
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayReprotAudit(List<Map<String, Object>> allLabel, List<BizLabel> bizTaxInstanceCategories) {
        long auditCount = bizTaxInstanceCategories.stream().filter(k -> Disabled.DISABLE.getValue() == k.getAudit()).count();
        if (auditCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.TaxAudit.AUDIT.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.TaxAudit.AUDIT.getSn());
                put(TaxLabelEnum.Lable.count.toString(), auditCount);
            }});
        }
        long unauditCount = bizTaxInstanceCategories.stream().filter(k -> Disabled.ENABLE.getValue() == k.getAudit()).count();
        if (unauditCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.TaxAudit.UN_AUDIT.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.TaxAudit.UN_AUDIT.getSn());
                put(TaxLabelEnum.Lable.count.toString(), unauditCount);
            }});
        }
    }

    /**
     * 列表报表确认
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayTaxAudit(List<Map<String, Object>> allLabel,  List<BizLabel> bizTaxInstanceCategories) {
        totalPayReprotAudit(allLabel, bizTaxInstanceCategories);
//        long auditCount = bizTaxInstanceCategories.stream().filter(k -> Disabled.DISABLE.getValue() == k.getAudit()).count();
//        if (auditCount > 0) {
//            allLabel.add(new HashMap<String, Object>(7) {{
//                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.ReportAudit.AUDIT.getDefSn());
//                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.ReportAudit.AUDIT.getSn());
//                put(TaxLabelEnum.Lable.count.toString(), auditCount);
//            }});
//        }
//        long unauditCount = bizTaxInstanceCategories.stream().filter(k -> Disabled.ENABLE.getValue() == k.getAudit()).count();
//        if (unauditCount > 0) {
//            allLabel.add(new HashMap<String, Object>(7) {{
//                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.ReportAudit.UN_AUDIT.getDefSn());
//                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.ReportAudit.UN_AUDIT.getSn());
//                put(TaxLabelEnum.Lable.count.toString(), unauditCount);
//            }});
//        }
    }


    /**
     * 列表申报
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayDeclareType(List<Map<String, Object>> allLabel,  List<BizLabel> bizTaxInstanceCategories) {

        //无需申报
        long undeclareCount = bizTaxInstanceCategories.stream().filter(k -> DeclareType.undeclare.equals(k.getDeclareType())).count();
        if (undeclareCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.DeclareType.UN_DECLARE.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.DeclareType.UN_DECLARE.getSn());
                put(TaxLabelEnum.Lable.count.toString(), undeclareCount);
            }});
        }
        //手工申报 == 手动申报
        long handworkCount = bizTaxInstanceCategories.stream().filter(k -> DeclareType.handwork.equals(k.getDeclareType())).count();
        if (undeclareCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.DeclareType.HAND_WORK.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.DeclareType.HAND_WORK.getSn());
                put(TaxLabelEnum.Lable.count.toString(), handworkCount);
            }});
        }
        //小微服申报 == 申报成功
        long yun9Count = bizTaxInstanceCategories.stream().filter(k -> DeclareType.yun9.equals(k.getDeclareType())).count();
        if (undeclareCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.DeclareType.YUN_9.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.DeclareType.YUN_9.getSn());
                put(TaxLabelEnum.Lable.count.toString(), yun9Count);
            }});
        }
        //税局申报==网上申报
        long taxOfficeCount = bizTaxInstanceCategories.stream().filter(k -> DeclareType.taxOffice.equals(k.getDeclareType())).count();
        if (undeclareCount > 0) {
            allLabel.add(new HashMap<String, Object>(7) {{
                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.DeclareType.TAX_OFFICE.getDefSn());
                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.DeclareType.TAX_OFFICE.getSn());
                put(TaxLabelEnum.Lable.count.toString(), taxOfficeCount);
            }});
        }

    }

    /**
     * 财务报表
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayTaxRecord(List<Map<String, Object>> allLabel,  List<BizLabel> bizTaxInstanceCategories) {
        List<Long> categoryIds = bizTaxInstanceCategories.stream().map(BizLabel::getId).collect(Collectors.toList());
        List<BizTaxInstanceCategoryFr> bizTaxInstanceCategoryFrs = bizTaxInstanceCategoryFrService.findByBizTaxInstanceCategoryIds(categoryIds);
        if (CollectionUtils.isNotEmpty(bizTaxInstanceCategoryFrs)) {
            //没有备案
            long noneCount = bizTaxInstanceCategoryFrs.stream().filter(k -> BizTaxInstanceCategoryFr.FrType.none.equals(k.getTaxOfficeFrType())).count();
            if (noneCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.TaxRecord.NONE.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.TaxRecord.NONE.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), noneCount);
                }});
            }
            if (bizTaxInstanceCategoryFrs.size() - noneCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.TaxRecord.RECORD.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.TaxRecord.RECORD.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), bizTaxInstanceCategoryFrs.size() - noneCount);
                }});
            }
        }
    }

    /**
     * 工资薪金个税 申报来源
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPaySourceType(List<Map<String, Object>> allLabel, List<BizLabel> bizTaxInstanceCategories) {
        Long[] categoryIds = bizTaxInstanceCategories.stream().map(BizLabel::getId).toArray(Long[]::new);
        List<String>  bizTaxInstanceCategoryPersonalPayrolls = bizTaxInstanceCategoryPersonalPayrollService.findByBizTaxInstanceCategoryIds(categoryIds);
        if (CollectionUtils.isNotEmpty(bizTaxInstanceCategoryPersonalPayrolls)) {
            //按上月
            long lastCount = bizTaxInstanceCategoryPersonalPayrolls.stream().filter(k -> BizTaxInstanceCategoryPersonalPayroll.SourceType.last.name().equals(k)).count();
            if (lastCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.SourceType.LAST_MONTH.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.SourceType.LAST_MONTH.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), lastCount);
                }});
            }
            //按实际
            long handCount = bizTaxInstanceCategoryPersonalPayrolls.stream().filter(k -> BizTaxInstanceCategoryPersonalPayroll.SourceType.hand.name().equals(k)).count();
            if (handCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.SourceType.PRACTICAL.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.SourceType.PRACTICAL.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), handCount);
                }});
            }
            //首次申报
            long firstCount = bizTaxInstanceCategoryPersonalPayrolls.stream().filter(k -> BizTaxInstanceCategoryPersonalPayroll.SourceType.first.name().equals(k)).count();
            if (firstCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.SourceType.FIRST.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.SourceType.FIRST.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), firstCount);
                }});
            }
        }
    }


    /**
     * 印花税 增值税状态
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayFhVatStateType(List<Map<String, Object>> allLabel,  List<BizLabel>bizTaxInstanceCategories) {
        List<Long> categoryIds = bizTaxInstanceCategories.stream().map(BizLabel::getId).collect(Collectors.toList());
        List<BizTaxInstanceCategoryYh> bizTaxInstanceCategoryYhs = bizTaxInstanceCategoryYhService.findByBizTaxInstanceCategoryIds(categoryIds);
        logger.debug("印花税数据{}", bizTaxInstanceCategoryYhs.size());
        if (CollectionUtils.isNotEmpty(bizTaxInstanceCategoryYhs)) {
            //未审核
            long unAuditCount = bizTaxInstanceCategoryYhs.stream().filter(k -> Disabled.ENABLE.getValue() == k.getVatAudit()).count();
            if (unAuditCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FhVatStateType.UN_AUDIT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FhVatStateType.UN_AUDIT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), unAuditCount);
                }});
            }
            //审核
            long auditCount = bizTaxInstanceCategoryYhs.stream().filter(k -> Disabled.DISABLE.getValue() == k.getVatAudit()).count();
            if (auditCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FhVatStateType.AUDIT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FhVatStateType.AUDIT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), auditCount);
                }});
            }
            //申报
            long successCount = bizTaxInstanceCategoryYhs.stream().filter(k -> BizTaxInstanceCategoryYh.VatState.success.equals(k.getVatState())).count();
            if (successCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FhVatStateType.SUCCESS.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FhVatStateType.SUCCESS.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), successCount);
                }});
            }
        }
    }

    /**
     * 附征税 增值税状态
     *
     * @param allLabel                 数据
     * @param bizTaxInstanceCategories 数据
     */
    private void totalPayFzDirectTaxStateType(List<Map<String, Object>> allLabel,  List<BizLabel> bizTaxInstanceCategories, TaxSn taxSn, BizTaxInstanceCategory.State state) {
        List<Long> categoryIds = bizTaxInstanceCategories.stream().map(BizLabel::getId).collect(Collectors.toList());
        List<BizTaxInstanceCategoryFz> bizTaxInstanceCategoryFzs = bizTaxInstanceCategoryFzService.findByBizTaxInstanceCategoryIds(categoryIds);
        logger.debug("印花税数据{}", bizTaxInstanceCategoryFzs.size());
        if (CollectionUtils.isNotEmpty(bizTaxInstanceCategoryFzs)) {
            //增值税销售额不大于10万
            if (TaxSn.m_fz.equals(taxSn)) {
                long mVatSaleAmountCount = bizTaxInstanceCategoryFzs.stream().filter(k -> new BigDecimal(100000).compareTo(k.getVatSaleAmount()) != -1).count();
                if (mVatSaleAmountCount > 0) {
                    allLabel.add(new HashMap<String, Object>(7) {{
                        put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_SALE_AMOUNT.getDefSn());
                        put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_SALE_AMOUNT.getSn());
                        put(TaxLabelEnum.Lable.count.toString(), mVatSaleAmountCount);
                    }});
                }
            }
            if (TaxSn.q_fz.equals(taxSn)) {
                long qVatSaleAmountCount = bizTaxInstanceCategoryFzs.stream().filter(k -> new BigDecimal(300000).compareTo(k.getVatSaleAmount()) != -1).count();
                if (qVatSaleAmountCount > 0) {
                    allLabel.add(new HashMap<String, Object>(7) {{
                        put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_SALE_AMOUNT_THREE.getDefSn());
                        put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_SALE_AMOUNT_THREE.getSn());
                        put(TaxLabelEnum.Lable.count.toString(), qVatSaleAmountCount);
                    }});
                }
            }

            //主税种未完成审核 消费税或增值税未审核
            long directTaxUnAuditCount = bizTaxInstanceCategoryFzs.stream().filter(k -> Disabled.ENABLE.getValue() == k.getVatAudit() || Disabled.ENABLE.getValue() == k.getSoqAudit()).count();
            if (directTaxUnAuditCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.DIRECT_TAX_UN_AUDIT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.DIRECT_TAX_UN_AUDIT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), directTaxUnAuditCount);
                }});
            }
            //主税种已审核 消费税和增值税已审核
            long directTaxAudittCount = bizTaxInstanceCategoryFzs.stream().filter(k -> Disabled.DISABLE.getValue() == k.getVatAudit() && Disabled.DISABLE.getValue() == k.getSoqAudit()).count();
            if (directTaxAudittCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.DIRECT_TAX_AUDIT.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.DIRECT_TAX_AUDIT.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), directTaxAudittCount);
                }});
            }
            //主税已申报 消费税和增值税已申报
            long directTaxSuccessCount = bizTaxInstanceCategoryFzs.stream().filter(k -> BizTaxInstanceCategoryFz.VatState.success.equals(k.getVatState()) && BizTaxInstanceCategoryFz.SoqState.success.equals(k.getSoqState())).count();
            if (directTaxSuccessCount > 0) {
                allLabel.add(new HashMap<String, Object>(7) {{
                    put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.DIRECT_TAX_SUCCESS.getDefSn());
                    put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.DIRECT_TAX_SUCCESS.getSn());
                    put(TaxLabelEnum.Lable.count.toString(), directTaxSuccessCount);
                }});
            }
        }
        //零申报
//        BizTaxMdCategory bizTaxMdCategory = null;
//        if (TaxSn.m_fz.equals(taxSn)) {
//            //得到税种
//            bizTaxMdCategory = Optional.ofNullable(bizTaxMdCategoryService.findBySn(TaxSn.m_vat)).orElse(null);
//        }
//        if (TaxSn.q_fz.equals(taxSn)) {
//            //得到税种
//            bizTaxMdCategory = Optional.ofNullable(bizTaxMdCategoryService.findBySn(TaxSn.q_vat)).orElse(null);
//        }
//        if (bizTaxMdCategory != null) {
////            List<BizTaxInstanceCategory> vatCategory = bizTaxInstanceCategoryService.findByBizTaxInstanceIdInAndStateAndBizTaxMdCategoryId(instances, state, bizTaxMdCategory.getId());
//            if (CollectionUtils.isNotEmpty(vatCategory)) {
//                long dvatZeroCount = vatCategory.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.zero.equals(k.getTaxDeclareType())).count();
//                if (dvatZeroCount > 0) {
//                    allLabel.add(new HashMap<String, Object>(7) {{
//                        put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_ZERO.getDefSn());
//                        put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_ZERO.getSn());
//                        put(TaxLabelEnum.Lable.count.toString(), dvatZeroCount);
//                    }});
//                }
//            }
//        long dvatZeroCount = bizTaxInstanceCategories.stream().filter(k -> BizTaxInstanceCategory.TaxDeclareType.zero.equals(k.getTaxDeclareType())).count();
//        if (dvatZeroCount > 0) {
//            allLabel.add(new HashMap<String, Object>(7) {{
//                put(TaxLabelEnum.Lable.key.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_ZERO.getDefSn());
//                put(TaxLabelEnum.Lable.value.toString(), TaxLabelEnum.FzDirectTaxStateType.VAT_ZERO.getSn());
//                put(TaxLabelEnum.Lable.count.toString(), dvatZeroCount);
//            }});
//        }

    }


}
