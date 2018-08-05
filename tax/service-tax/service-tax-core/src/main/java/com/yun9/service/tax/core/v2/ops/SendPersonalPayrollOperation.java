package com.yun9.service.tax.core.v2.ops;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.AbstractTaxRouterBuilder;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import com.yun9.service.tax.core.v2.handler.TaxRouterTaxCategoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 个税工资薪金
 *
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-06-11 18:01
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_personal_payroll, taxOffice = TaxOffice.ds, cycleType = CycleType.m)
}
)
public class SendPersonalPayrollOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {


    private final static Logger logger = LoggerFactory.getLogger(SendPersonalPayrollOperation.class);

    @Autowired
    TaskSendHandler taskSendHandler;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    BizTaxDeclareService bizTaxDeclareService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

    @Autowired
    TaxRouterTaxCategoryHelper taxRouterTaxCategoryHelper;

    /**
     * 税务路由组织数据
     *
     * @param context
     * @return
     */
    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        HashMap rst =  new HashMap<String, Object>(11) {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getMonth());
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
            put("taxclosingdate", context.getBizTaxInstanceCategory().getCloseDate()* 1000); //申报截止时间
            put("datas", buildDatas(context.getBizTaxInstanceCategory().getId()));
        }};
        logger.info("-->发送到税务路由{}", JSON.toJSON(rst));

        return rst;
    }

    /**
     * 发起操作
     *
     * @param context
     * @return
     */
    @Override
    public Object process(OperationContext context) {


        //================申报实例申报前状态校验==========================
        logger.debug("----开始进行申报前的状态检查---");
        if (!context.getBizTaxInstanceCategory().prepare2Send()) {
            logger.error("申报状态错误",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(),context.getBizTaxInstanceCategory().isAudit()?"已审核":"未审核"));
        }

        if (!taxRouterTaxCategoryHelper.isInDeclareRangeTime(context.getBizTaxInstanceCategory())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, "该税种不在申报期");
        }

        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(
                bizTaxInstanceCategoryPersonalPayrollService
                        .findByInstanceCategoryId(context.getBizTaxInstanceCategory().getId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该个税工资薪金申报记录"));

        //检查操作状态是否是"按实际"
        if (bizTaxInstanceCategoryPersonalPayroll.getSourceType() != BizTaxInstanceCategoryPersonalPayroll.SourceType.hand) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "任务发起失败,当前操作状态不是\"按实际\"");
        }

        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));
        //====================调用申报发起成功========================================================
        bizTaxDeclareService.startDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        return "任务发起成功";
    }


    private List<HashMap> buildDatas(final Long bizTaxInstanceCategoryId) {

        logger.debug("开始组装数据---");

        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategoryId))
                .orElseThrow(() -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该个税工资薪金申报记录"));

        List<BizTaxInstanceCategoryPersonalPayrollItem> items = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollItemService.findByCategoryPersonalPayrollId(bizTaxInstanceCategoryPersonalPayroll.getId()))
                .orElseThrow(() -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该个税工资薪金人员信息记录"));

        //检查人是否有人员清单
        if (CollectionUtils.isEmpty(items)) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误,个人所得税工资薪金按实际申报需要传递人员清单");
        }

        return new ArrayList<HashMap>(items.size()) {
            {
                items.forEach(v->{
                    add(new HashMap(){{
                        put("a1","");
                        put("a2",v.getSort());//序号
                        put("a3",Optional.ofNullable(v.getName()).orElse(""));//姓名
                        put("a4",Optional.ofNullable(v.getCountryid()).orElse(""));//国家代码
                        put("a5",Optional.ofNullable(v.getCardtype()).orElse(""));//国家名称
                        put("a6",Optional.ofNullable(v.getCardnumber()).orElse(""));//身份证号
                        put("a7",Optional.ofNullable(v.getBegindate()).orElse(""));//申报期起
                        put("a8",Optional.ofNullable(v.getEnddate()).orElse(""));//申报截止
                        put("a9",Optional.ofNullable(v.getItemcode()).orElse(""));//征税品目代码
                        put("a10",Optional.ofNullable(v.getWage()).orElse(BigDecimal.ZERO).doubleValue());//收入额
                        put("a11",Optional.ofNullable(v.getDutyfreeamount()).orElse(BigDecimal.ZERO).doubleValue());//免税所得
                        put("a12",Optional.ofNullable(v.getPension()).orElse(BigDecimal.ZERO).doubleValue());//基本养老费
                        put("a13",Optional.ofNullable(v.getHealthinsurance()).orElse(BigDecimal.ZERO).doubleValue());//基本医保费
                        put("a14",Optional.ofNullable(v.getUnemploymentinsurance()).orElse(BigDecimal.ZERO).doubleValue());//失业保险费
                        put("a15",Optional.ofNullable(v.getHousingfund()).orElse(BigDecimal.ZERO).doubleValue());//住房公积金
                        put("a16",Optional.ofNullable(v.getOriginalproperty()).orElse(BigDecimal.ZERO).doubleValue());//财产原值
                        put("a17",Optional.ofNullable(v.getAllowdeduction()).orElse(BigDecimal.ZERO).doubleValue());//允许扣除的税费
                        put("a18",Optional.ofNullable(v.getOther()).orElse(BigDecimal.ZERO).doubleValue());//其他
                        put("a19",Optional.ofNullable(v.getTotal()).orElse(BigDecimal.ZERO).doubleValue());//合计
                        put("a20",Optional.ofNullable(v.getDeductionamount()).orElse(BigDecimal.ZERO).doubleValue());//减除费用
                        put("a21",Optional.ofNullable(v.getDeductionamount()).orElse(BigDecimal.ZERO).doubleValue());//减除费用
                        put("a22",Optional.ofNullable(v.getDeductiondonate()).orElse(BigDecimal.ZERO).doubleValue());//准予扣除的捐赠额
                        put("a23",Optional.ofNullable(v.getTaxincome()).orElse(BigDecimal.ZERO).doubleValue());//应纳税所得额
                        put("a24",Optional.ofNullable(v.getTaxrate()).orElse(BigDecimal.ZERO).doubleValue());//税率
                        put("a25",Optional.ofNullable(v.getTaxrate()).orElse(BigDecimal.ZERO).doubleValue());//税率
                        put("a26",Optional.ofNullable(v.getSpeeddeduction()).orElse(BigDecimal.ZERO).doubleValue());//速算扣除数
                        put("a27",Optional.ofNullable(v.getShouldpaytax()).orElse(BigDecimal.ZERO).doubleValue());//应纳税额
                        put("a28",Optional.ofNullable(v.getRelieftax()).orElse(BigDecimal.ZERO).doubleValue());///减免税额
                        put("a29",Optional.ofNullable(v.getShouldcosttax()).orElse(BigDecimal.ZERO).doubleValue());//应扣缴税额
                        put("a30",Optional.ofNullable(v.getAlreadycosttax()).orElse(BigDecimal.ZERO).doubleValue());//已扣缴税额
                        put("a31",Optional.ofNullable(v.getFinallytax()).orElse(BigDecimal.ZERO).doubleValue());//应补（退）税额
                        put("a32",Optional.ofNullable(v.getRemarks()).orElse(""));//备注
                        put("a33",Optional.ofNullable(v.getAnnuity()).orElse(BigDecimal.ZERO).doubleValue());//年金
                        put("a34",Optional.ofNullable(v.getInsurance()).orElse(BigDecimal.ZERO).doubleValue());//商业健康险
                        put("a35",Optional.ofNullable(v.getDeduction()).orElse(BigDecimal.ZERO).doubleValue());//投资抵扣
                        put("a36",Optional.ofNullable(v.getAlreadydeclarewage()).orElse(BigDecimal.ZERO).doubleValue());//已申报金额
                        put("a37",Optional.ofNullable(v.getTaxwage()).orElse(BigDecimal.ZERO).doubleValue());//含税收入额
                        put("a38",Optional.ofNullable(v.getTaxburdentype()).orElse(""));//税款负担方式
                        put("a39",Optional.ofNullable(v.getCompanyrate()).orElse(BigDecimal.ZERO));//雇主负担比例
                        put("a40",Optional.ofNullable(v.getCompanyamount()).orElse(BigDecimal.ZERO).doubleValue());//雇主负担金额
                        put("a41",Optional.ofNullable(v.getDetailname()).orElse(""));//征税品目名称
                    }});
                });
                logger.debug("组装完成-->{}", this);

            }
        };
    }

}
