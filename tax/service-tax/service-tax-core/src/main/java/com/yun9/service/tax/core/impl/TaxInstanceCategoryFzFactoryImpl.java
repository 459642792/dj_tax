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
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;


import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryFzFactory;

import com.yun9.service.tax.core.dto.BizTaxFzItemDTO;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;

import com.yun9.service.tax.core.ft.ops.TaxFzOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import javax.swing.text.html.Option;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/5/7.
 */
@Component
public class TaxInstanceCategoryFzFactoryImpl implements TaxInstanceCategoryFzFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryFzFactoryImpl.class);


    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    PageCommon pageCommon;
    //    @Autowired
//    BizTaxInstanceCategoryFzItemService bizTaxInstanceCategoryFzItemService;
    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;

    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizTaxInstanceCategoryFzItemService bizTaxInstanceCategoryFzItemService;

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
        Pagination<BizTaxInstanceCategoryFz> pageObj = bizTaxInstanceCategoryFzService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
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

        //3 获取reportCheckStates
        List<BizTaxInstanceCategoryFzItem> items = bizTaxInstanceCategoryFzItemService.findByBizTaxInstanceCategoryFzIds(ids);
        HashMap itemsMap = new HashMap<Long, List<BizTaxInstanceCategoryFzItem>>() {{
            items.forEach(e -> {
                if (e != null) {
                    List<BizTaxInstanceCategoryFzItem> fzItemList = get(e.getBizTaxInstanceCategoryFzId());
                    if (fzItemList == null) {
                        fzItemList = new ArrayList();
                    }
                    fzItemList.add(e);
                    put(e.getBizTaxInstanceCategoryFzId(), fzItemList);
                }
            });
        }};

        //6 机构客户状态
        Map<Long, BizMdInstClient> bizMdInstClientsMap = new HashMap<Long, BizMdInstClient>() {{
            bizMdInstClients.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};


        //pageCommon.getMdInstClientsMap(instClientIds);

        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{
                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));

                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap,bizMdInstClientsMap,null,deductsMap,banksMap,v.getBizTaxInstanceCategory()));


                    //todo 附征税 （未完整）
                    put("id", v.getId()); //附征税ID
                    put("vatState",v.getVatState());//增值税状态[none未申报][success已申报]
                    put("vatSaleAmount",v.getVatSaleAmount());//增值税销售额
                    put("vatTaxAmount",v.getVatTaxAmount());////增值税额
                    put("soqState", v.getSoqState()); //消费税状态[none未申报][success已申报]
                    put("soqTaxAmount", v.getSoqTaxAmount()); //增值税销售额
                    put("businessTaxState", v.getBusinessTaxState()); //营业税状态[none未申报][success已申报]
                    put("businessTaxAmount", v.getBusinessTaxAmount()); //营业税额

                    if (itemsMap.get(v.getId()) != null) {
                        put("reportCheckState", Optional.ofNullable(itemsMap.get(v.getId()) ).map((value) -> 1).orElse(0));//申报表
                    }else{
                        put("reportCheckState",0);//申报表
                    }

                }});
            });
        }});
        return pagination;

    }

    @Override
    public Optional<List<BizTaxInstanceCategoryFzItem>> itemList(long instanceCategoryFzId) {
        List<BizTaxInstanceCategoryFzItem> backItemList = new ArrayList<BizTaxInstanceCategoryFzItem>();
        List<BizTaxInstanceCategoryFzItem> itemList = Optional.ofNullable(bizTaxInstanceCategoryFzItemService.findByBizTaxInstanceCategoryFzId(instanceCategoryFzId)).orElse(Collections.EMPTY_LIST);
        itemList.stream().forEach(item->{
            if (item.getDisabled() == 0){
                backItemList.add(item);
            }
        });
        return Optional.ofNullable(backItemList);
    }

    @Override
    public List<BizTaxInstanceCategoryFzItem> saveItem(long instanceCategoryFzId, List<BizTaxInstanceCategoryFzItem> itemList) throws IllegalAccessException {
        if (CollectionUtils.isNotEmpty(itemList)) {
            for (int i=0;i<itemList.size();i++){
                BizTaxInstanceCategoryFzItem item = itemList.get(i);
                //校验
                TaxFzOperation.checkFzItemsData(item);
                TaxFzOperation.checkIfLessZero(item);
            }
        }
        return bizTaxInstanceCategoryFzItemService.save(instanceCategoryFzId,itemList);
    }
    @Override
    public void unconfirmed(long bizTaxInstanceCategoryFzId, long userId) {
        if (StringUtils.isEmpty(bizTaxInstanceCategoryFzId)){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "参数不能为空");
        }
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = Optional.ofNullable(
                bizTaxInstanceCategoryFzService.findById(bizTaxInstanceCategoryFzId)
        ).orElseThrow(() ->
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到附征税申报实例")
        );
        //判断可用
        if ( !bizTaxInstanceCategoryFz.isEnable()){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryFz.getBizTaxInstanceCategory();
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

    @Override
    public Map<String, Object> getVatAndSoq(long bizTaxInstanceCategoryFzId) {
        if (StringUtils.isEmpty(bizTaxInstanceCategoryFzId) ){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "参数不能为空"); 
        }
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = Optional.ofNullable(
                bizTaxInstanceCategoryFzService.findById(bizTaxInstanceCategoryFzId)
        ).orElseThrow(() ->
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到附征税申报实例")
        );
        //判断可用
        if ( !bizTaxInstanceCategoryFz.isEnable()){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
        }
       
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryFz.getBizTaxInstanceCategory();
        if (null == bizTaxInstanceCategory){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "申报实例不存在!");
        }
        if (bizTaxInstanceCategory.isAudit()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "操作失败,當前附征税已审核,不可操作!");
        }
        return bizTaxInstanceCategoryFzService.getVatAndSoq(bizTaxInstanceCategoryFzId);
    }

    @Override
    public void batchAudit(List<Long> ids, long userId) {
        try {
            logger.debug("批量审核开始");
            for (long id : ids){
                logger.debug("当前附征税id为{}",id);
                BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = Optional.ofNullable(
                        bizTaxInstanceCategoryFzService.findById(id)
                ).orElseThrow(() ->
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到附征税申报实例")
                );
                //判断可用
                if ( !bizTaxInstanceCategoryFz.isEnable()){
                    BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
                }
               
                List<BizTaxInstanceCategoryFzItem> items =
                        bizTaxInstanceCategoryFzItemService.findByBizTaxInstanceCategoryFzId(id);

                if (null != items && items.size() > 0) {
                    for (BizTaxInstanceCategoryFzItem fzItem : items) {

                        TaxFzOperation.checkIfLessZero(fzItem);

                        TaxFzOperation.checkFzItemsData(fzItem);
                    }
                }else {
                    BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税征收项目至少需要一条,错误id"+id); 
                }
                BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryFz.getBizTaxInstanceCategory();
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
                logger.debug("审核附征税id为{}，审核通过",id);
            } 
        }catch (IllegalAccessException e){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "校验出错");
        }
        logger.debug("批量审核完成");
    }

    @Override
    public void confirmed(BizTaxFzItemDTO bizTaxFzItemDTO , long userId) {
        if (null == bizTaxFzItemDTO || bizTaxFzItemDTO.getFzId() == 0){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "参数不能为空");
        }
        //传入数据审核
        checkFzItems(bizTaxFzItemDTO);
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = Optional.ofNullable(
                bizTaxInstanceCategoryFzService.findById(bizTaxFzItemDTO.getFzId())
        ).orElseThrow(() ->
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到附征税申报实例")
        );
        //判断可用
        if ( !bizTaxInstanceCategoryFz.isEnable()){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryFz.getBizTaxInstanceCategory();
        if (bizTaxInstanceCategory.isAudit()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "税种已审核,无需再次审核");
        }
     
        //保存item数据
        bizTaxInstanceCategoryFzItemService.save(bizTaxFzItemDTO.getFzId(),bizTaxFzItemDTO.getFzItems());
        BigDecimal payTax = BigDecimal.valueOf(0.0);
        for (BizTaxInstanceCategoryFzItem bizTaxInstanceCategoryFzItem : bizTaxFzItemDTO.getFzItems()){
            payTax = payTax.add(bizTaxInstanceCategoryFzItem.getTaxShouldPayAmount());
        }
        //保存应补（退）税额
        bizTaxInstanceCategory.setTaxPayAmount(payTax);
        bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);
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
    }
    //校验
    private  List<BizTaxInstanceCategoryFzItem> checkFzItems(BizTaxFzItemDTO bizTaxFzItemDTO){
        List<BizTaxInstanceCategoryFzItem> resultList = new ArrayList<>();
        if (null == bizTaxFzItemDTO.getFzItems()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "附征税征收项目至少需要一条");
        }
        if (bizTaxFzItemDTO.getFzItems().size() > 0 ){
            bizTaxFzItemDTO.getFzItems().forEach( v ->{
                try {
                    //验证非空
                    checkFzItemsDataIfNull (v);
                    //验证id匹配
                    if (v.getBizTaxInstanceCategoryFzId() != bizTaxFzItemDTO.getFzId()){
                        BizTaxException
                                .throwException(BizTaxException.Codes.BizTaxException, "该条附征税征收明细与当前附征税申报实例不匹配");
                    }
                    //验证数据不小于0
                    TaxFzOperation.checkIfLessZero(v);
                }catch (IllegalAccessException e){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "校验出错");
                }
                // 验证数据正确性
                TaxFzOperation.checkFzItemsData(v);
            });
        }else {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "附征税征收项目至少需要一条");
        }
        return resultList;
    }
    private void checkFzItemsDataIfNull(BizTaxInstanceCategoryFzItem obj) throws IllegalAccessException{
        List<String > errors = new ArrayList<>();
        if (null == obj ){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "校验数据为空");
           
        }else {
            if (obj.getBizTaxInstanceCategoryFzId() == 0 ){
                errors.add("关联附征税id不能为空");
            }
            if (obj.getId() == 0){
                errors.add("附征税明细id为空!");
            }
            //减免性质代码为空,减免额为0
            if (StringUtils.isEmpty(obj.getTaxRemitCode())){
                if (StringUtils.isNotEmpty(obj.getTaxRemitAmount()) &&
                        obj.getTaxRemitAmount().compareTo(BigDecimal.ZERO) != 0){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "减免性质代码为空,减免额只能为0!");
                }
            }
            //减免性质代码不为空,减免额需大于0
            if (StringUtils.isNotEmpty(obj.getTaxRemitCode())){
                if (StringUtils.isEmpty(obj.getTaxRemitAmount()) ||
                        obj.getTaxRemitAmount().compareTo(BigDecimal.ZERO) != 1){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "减免性质代码不为空,减免额需大于0!");
                    
                }
            }
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            for (Field f : declaredFields){
                f.setAccessible(true);
                if (null == f.get(obj) || "".equals(f.get(obj))){
                    switch (f.getName()){
                        case "itemCode":
                            errors.add("征收项目代码不能为空");
                            break;
                        case "itemDetailCode":
                            errors.add("征收品目代码不能为空");
                            break;
                        case "saleAmountVatNormal":
                            errors.add("增值税(一般增税)不能为空");
                            break;
                        case "saleAmountVatFree":
                            errors.add("增值税(免抵税额)不能为空");
                            break;
                        case "saleAmountVatSoq":
                            errors.add("消费税不能为空");
                            break;
                        case "saleAmountVatBusiness":
                            errors.add("营业税不能为空");
                            break;
                        case "saleAmountTotal":
                            errors.add("合计不能为空");
                            break;
                        case "taxRate":
                            errors.add("税率(征收率)不能为空");
                            break;
                        case "taxPayAmount":
                            errors.add("本期应纳税(费)额不能为空");
                            break;
                       /* case "taxRemitCode":
                            errors.add("减免性质代码不能为空");
                            break;*/
                        case "taxRemitAmount":
                            errors.add("减免额不能为空");
                            break;
                        case "taxAlreadyPayAmount":
                            errors.add("本期已缴税(费)额不能为空");
                            break;
                        case "taxShouldPayAmount":
                            errors.add("本期应补(退(税(费)额不能为空");
                            break;

                    }
                }
            }
        }
      if (errors.size() > 0){
            String error = "";
            for (String err : errors){
                error += err + ",";
            }
          BizTaxException
                  .throwException(BizTaxException.Codes.BizTaxException, error);
      }

    }

}
