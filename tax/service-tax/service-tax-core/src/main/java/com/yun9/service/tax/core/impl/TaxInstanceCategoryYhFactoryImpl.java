package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryYhFactory;
import com.yun9.service.tax.core.dto.BizTaxYhItemDTO;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.ft.ops.TaxFzOperation;
import com.yun9.service.tax.core.ft.ops.TaxYhOperation;
import com.yun9.service.tax.core.dto.BizTaxInstanceCategoryYhItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/5/7.
 */
@Component
public class TaxInstanceCategoryYhFactoryImpl implements TaxInstanceCategoryYhFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryYhFactoryImpl.class);


    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    BizTaxInstanceCategoryYhService bizTaxInstanceCategoryYhService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    PageCommon pageCommon;
    
    @Autowired
    BizTaxInstanceCategoryYhItemService bizTaxInstanceCategoryYhItemService;

    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;

    @Autowired
    BizMdInstClientService bizMdInstClientService;


    @Override
    public Pagination<HashMap> pageByState(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state, int page, int limit, Map<String, Object> params) {
        Pagination pagination = new Pagination();
        pagination.setContent(new ArrayList());

        //1 检查组织id
        List<BizMdInstClient> bizMdInstClients = pageCommon.getBizMdInstClients(orgTreeId, params);
        if (CollectionUtils.isEmpty(bizMdInstClients)) {
            return pagination;
        }

        List<Long> instClientIds = bizMdInstClients.stream().map(v -> v.getId()).collect(Collectors.toList());
        Pagination<BizTaxInstanceCategoryYh> pageObj = bizTaxInstanceCategoryYhService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());


        //组织参数
        List<Long> companyIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()).collect(Collectors.toList());
        List<Long> instanceCategoryIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getId()).collect(Collectors.toList());
        List<Long> ids = pageObj.getContent().stream().map(v -> v.getId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(instanceCategoryIds)) {
            return pagination;
        }

        CompletableFuture<Map<String, CompanyAccountDTO>> companyAccountMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getCompanyAccountDTOS(companyIds));
        CompletableFuture<Map<Long, List<BizTaxInstanceCategoryDeduct>>> deductsMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getDeductsMap(instanceCategoryIds));
        CompletableFuture<Map<String, List<BizTaxCompanyBank>>> banksMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getBanksMap(companyIds));
        CompletableFuture<Void> allOf = CompletableFuture.allOf(companyAccountMapFuture, deductsMapFuture, banksMapFuture).whenComplete((v, th) -> {
            if (th != null) {
                throw new ServiceTaxException(th.getMessage());
            }
        });
        allOf.join();


        Map<String, CompanyAccountDTO> companyAccountMap = companyAccountMapFuture.join();
        Map<Long, List<BizTaxInstanceCategoryDeduct>> deductsMap = deductsMapFuture.join();
        Map<String, List<BizTaxCompanyBank>> banksMap = banksMapFuture.join();

        //6 机构客户状态
        Map<Long, BizMdInstClient> bizMdInstClientsMap = new HashMap<Long, BizMdInstClient>() {{
            bizMdInstClients.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};
        //7 征收品目
        List<BizTaxInstanceCategoryYhItem> items = bizTaxInstanceCategoryYhItemService.findByBizTaxInstanceCategoryYhIds(ids);
        HashMap yhItemsMap = new HashMap<Long, List<HashMap>>() {{
            items.forEach(e -> {
                if (e != null) {
                    List<HashMap> yhItemList = get(e.getBizTaxInstanceCategoryYhId());
                    if (yhItemList == null) {
                        yhItemList = new ArrayList();
                    }
                    yhItemList.add(new HashMap(){{
                        put("itemCode",e.getItemCode());
                        put("id",e.getId());
                    }});
                    put(e.getBizTaxInstanceCategoryYhId(), yhItemList);
                }
            });
        }};

        //6 组织数据
        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{
                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));


                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap,bizMdInstClientsMap,null,deductsMap,banksMap,v.getBizTaxInstanceCategory()));


                    //todo 印花税 （未完整）
                    put("id", v.getId()); //印花税种ID
                    put("vatState", v.getVatState()); //增值税状态[none未申报][success已申报]
                    put("vatSaleAmount", v.getVatSaleAmount()); //增值税销售额
                    put("items",yhItemsMap.get(v.getId()));//征收品目

                    //报表
                    if (yhItemsMap.get(v.getId()) != null) {
                        put("reportCheckState", Optional.ofNullable(yhItemsMap.get(v.getId()) ).map((value) -> 1).orElse(0));//申报表
                    }else{
                        put("reportCheckState",0);//申报表
                    }
                }});
            });
        }});
        return pagination;

    }

    @Override
    public BizTaxInstanceCategoryYhItemDTO addYhItem(List<BizTaxInstanceCategoryYhItem> yhItemList) {



        BizTaxInstanceCategoryYhItemDTO result = new BizTaxInstanceCategoryYhItemDTO();

        List<String> message = new ArrayList<>();

        for (int i = 0;i<yhItemList.size();i++) {

            try {

                //分开大于0校验
                TaxYhOperation.checkIfNullAndLessZero(yhItemList.get(i));

                //校验数据
                TaxYhOperation.checkDataCount(yhItemList.get(i));


            }catch (Exception e) {

                message.add("第"+ (i+1) +"条数据校验错误:"+ e.getMessage());

            }

        }

        if (message.size() > 0) {

            result.setCode(500);
            result.setMessage(message);

            return result;

        }



        bizTaxInstanceCategoryYhItemService.batchAddYhItem(yhItemList);

        result.setCode(200);
        result.setMessage(new ArrayList() {{
            add("保存成功");
        }});


        return result;
    }

    @Override
    public Object getYhItem(int page, int limit, Map<String, Object> params) {

        Map<String, Object> result = new HashMap();

        List<String> content = new ArrayList<>();

        try {

            Pagination<BizTaxInstanceCategoryYhItem> valueList = bizTaxInstanceCategoryYhItemService.pageByCondition(page,limit,params);

            content.add("查询成功");

            result.put("code", 200);
            result.put("message", content);
            result.put("content", valueList);


        } catch (Exception e) {

            e.printStackTrace();

            content.add("查询失败:" + e.getMessage());

            result.put("code", 200);
            result.put("message", content);

        }

        return result;

    }

    @Override
    public Map<String, Object> getVat(long bizTaxInstanceCategoryYhId) {
        if (StringUtils.isEmpty(bizTaxInstanceCategoryYhId) || bizTaxInstanceCategoryYhId == 0){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "操作失败,参数为空!");
        }
        BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = bizTaxInstanceCategoryYhService.findById(bizTaxInstanceCategoryYhId);
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryYh.getBizTaxInstanceCategory();
        if (null == bizTaxInstanceCategoryYh || null == bizTaxInstanceCategory){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "操作失败,税种实例为空!");
        }
        //if ( !BizTaxInstanceCategoryYh.VatState.success.equals(bizTaxInstanceCategoryYh.getVatState())){
        //    BizTaxException
        //            .throwException(BizTaxException.Codes.BizTaxException, "操作失败,相关增值税未申报!");
        //}
        if ( !bizTaxInstanceCategoryYh.isEnable()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "操作失败,该印花税实例不可用!");
        }
        if (bizTaxInstanceCategory.isAudit()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "操作失败,當前印花稅已审核,不可操作!");
        }
        return new HashMap<String,Object>(){{
            put("vatSaleAmount",bizTaxInstanceCategoryYhService.getVatSaleAmount(bizTaxInstanceCategoryYhId));
        }};
    }

    @Override
    public void batchAudit(List<Long> ids, long userId) {
        try {
            logger.debug("批量审核开始");
            for (long id : ids){
                logger.debug("当前印花税id为：{}",id);
                BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = Optional.ofNullable(
                        bizTaxInstanceCategoryYhService.findById(id)
                ).orElseThrow(() ->
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到印花税申报实例")
                );
                //判断可用
                if ( !bizTaxInstanceCategoryYh.isEnable()){
                    BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "印花税申报实例不可用");
                }

                List<BizTaxInstanceCategoryYhItem> items =
                        bizTaxInstanceCategoryYhItemService.findByBizTaxInstanceCategoryYhId(id);

                if (null != items && items.size() > 0) {
                    for (BizTaxInstanceCategoryYhItem yhItem : items) {
                        //非空校验
                        TaxYhOperation.checkIfNullAndLessZero(yhItem);
                        //逻辑校验
                        TaxYhOperation.checkDataCount(yhItem);
                      
                    }
                }else {
                    BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "印花税征收项目至少需要一条,错误id："+id);
                }
                BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryYh.getBizTaxInstanceCategory();
                if (bizTaxInstanceCategory.isAudit()){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "税种已审核,无需再次审核");
                }
                //调用审核
                //统一审核
                taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
                    @Override
                    public void success() {
                        //审核成功
                    }
                    @Override
                    public void exception(BizException ex) {
                        throw ex;
                    }
                });
                logger.debug("审核印花税id为：{}，审核通过",id);
            }
        }catch (IllegalAccessException e){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "校验出错");
        }
        logger.debug("批量审核完成");
    }

    @Override
    public BizTaxInstanceCategoryYhItemDTO vaild(BizTaxInstanceCategoryYhItem yhItem) {

        BizTaxInstanceCategoryYhItemDTO bizTaxInstanceCategoryYhItemDTO = new BizTaxInstanceCategoryYhItemDTO();

        bizTaxInstanceCategoryYhItemDTO.setCode(0);

        List<String> error = new ArrayList<>();

        if(StringUtils.isEmpty(yhItem.getBizTaxInstanceCategoryYhId())) {
            error.add("税种实例编号为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getItemCode())) {
            error.add("征税项目代码为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxBase())) {
            error.add("计税依据为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getApprovAmount())) {
            error.add("核定金额为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getApprovRate())) {
            error.add("核定比率为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxRate())) {
            error.add("适用税率为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxPayAmount())) {
            error.add("应纳税额为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxAlreadyPayAmount())) {
            error.add("已扣缴金额为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxRemitCode())) {
            error.add("减免代码为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxRemitAmount())) {
            error.add("减免金额为空,请检查!");
        }
        if(StringUtils.isEmpty(yhItem.getTaxShouldPayAmount())) {
            error.add("本期应补(退(税(费)额为空,请检查!");
        }


        if(error.size() > 0) {

            bizTaxInstanceCategoryYhItemDTO.setCode(1);
            bizTaxInstanceCategoryYhItemDTO.setMessage(error);

        }


        return bizTaxInstanceCategoryYhItemDTO;
    }

    @Override
    public void confirmed(BizTaxYhItemDTO bizTaxYhItemDTO, long userId) {
        //验参
        checkData(bizTaxYhItemDTO);
        
        BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = bizTaxInstanceCategoryYhService.findById(bizTaxYhItemDTO.getYhId());
        if (null == bizTaxInstanceCategoryYh){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "税种实例为空!");  
        }
       
        if ( !bizTaxInstanceCategoryYh.isEnable()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "该申报实例不可用!");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryYh.getBizTaxInstanceCategory();
        if (null == bizTaxInstanceCategory){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "该公司无申报实例"); 
        }
        if (bizTaxInstanceCategory.isAudit()){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "该公司已经审核,无需再次审核");
        }
        //保存item
        // bizTaxYhItemDTO.getYhItems().forEach(bizTaxInstanceCategoryYhItemService::update); 
        bizTaxInstanceCategoryYhItemService.batchAddYhItem(bizTaxYhItemDTO.getYhItems());
        //保存应补（退）税额
        BigDecimal payTax = BigDecimal.valueOf(0.0);
        for (BizTaxInstanceCategoryYhItem bizTaxInstanceCategoryYhItem : bizTaxYhItemDTO.getYhItems()){
            payTax = payTax.add(bizTaxInstanceCategoryYhItem.getTaxShouldPayAmount());
        }
        bizTaxInstanceCategory.setTaxPayAmount(payTax);
        bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);
        //统一审核
        taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
            @Override
            public void success() {
                
            }

            @Override
            public void exception(BizException ex) {

            }
        });
        
    }

    @Override
    public void unconfirmed(long bizTaxInstanceCategoryYhId, long userId) {
        if (StringUtils.isEmpty(bizTaxInstanceCategoryYhId)){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "参数不能为空");
        }
        BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = Optional.ofNullable(
                bizTaxInstanceCategoryYhService.findById(bizTaxInstanceCategoryYhId)
        ).orElseThrow(() ->
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到印花税申报实例")
        );
        //判断可用
        if ( !bizTaxInstanceCategoryYh.isEnable()){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryYh.getBizTaxInstanceCategory();
        if (null != bizTaxInstanceCategory){
            if ( !bizTaxInstanceCategory.isAudit()){
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "税种未审核,无需撤销审核");
            }
            //统一撤销
            taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(),userId);
        }else {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "关联申报实例为空");
        }
    }


    private void checkData(BizTaxYhItemDTO bizTaxYhItemDTO){
        if (null == bizTaxYhItemDTO){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "参数为空!");
        }
        //
        if (bizTaxYhItemDTO.getYhItems().size() > 0 ){
            bizTaxYhItemDTO.getYhItems().forEach(v ->{
               
                try {
                    //非空校验 //数字大于0校验
                    TaxYhOperation.checkIfNullAndLessZero(v);
                    //纳税凭证进行匹配校验,待建表
                    //数据逻辑校验
                    TaxYhOperation.checkDataCount(v);
                }catch (IllegalAccessException e){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "校验出错!"); 
                }
                if (v.getBizTaxInstanceCategoryYhId() != bizTaxYhItemDTO.getYhId()){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "印花税征收明细与当前印花税申报实例不匹配!");
                }
            });
        }else {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "印花税征收明细至少有一条!");
        }
    }
  
}
