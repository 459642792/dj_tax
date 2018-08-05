package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.BizMdAreaService;
import com.yun9.biz.md.BizMdDictionaryCodeService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdArea;
import com.yun9.biz.md.domain.entity.BizMdDictionaryCode;
import com.yun9.biz.md.enums.CycleType;
import com.yun9.biz.md.enums.TaxOffice;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalMFactory;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollItemFactory;
import com.yun9.service.tax.core.dto.BizTaxInstanceCategoryPersonalPayrollItemStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yunjie
 * @version 1.0
 * @since 2018-05-30 16:53
 */

/**
 * 更新记录
 * 1. 2018-06-05 15:00 新增校验当前人员的工资所属期是否在当前公司以前年度申报过
 * 2. 2018-06-06 11:00 新增校验不能小于0的数值和新增一个初始化的方法
 */
@Component
public class TaxInstanceCategoryPersonalPayrollItemFactoryImpl implements TaxInstanceCategoryPersonalPayrollItemFactory {
    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;
    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxInstanceService bizTaxInstanceService;
    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;
    @Autowired
    BizTaxMdCategoryService bizTaxMdCategoryService;
    @Autowired
    BizMdAreaService bizMdAreaService;
    @Autowired
    private BizMdDictionaryCodeService bizMdDictionaryCodeService;

    @Autowired
    TaxInstanceCategoryPersonalMFactory taxInstanceCategoryPersonalMFactory;

    private static final Map<String, String> personalitemcodehash = new ConcurrentHashMap<>();
    private static final Map<String, String> countryareahash = new ConcurrentHashMap<>();
    private static final Map<String, String> cardclasshash = new ConcurrentHashMap<>();

    @Override
    //验证个税人员清单
    public BizTaxInstanceCategoryPersonalPayrollItemStateDTO vaild(BizTaxInstanceCategoryPersonalPayrollItem personalItem, long mdCompanyId, long mdInstClientId) {

        BizTaxInstanceCategoryPersonalPayrollItemStateDTO bizTaxInstanceCategoryPersonalPayrollItemStateDTO = new BizTaxInstanceCategoryPersonalPayrollItemStateDTO();
        bizTaxInstanceCategoryPersonalPayrollItemStateDTO.setCode(0);
        List<String> error = new ArrayList<>();
        //判断数据
        if (personalItem.getName() == null || personalItem.getName().equals("")) error.add("姓名不能为空,请检查!");
        if (personalItem.getWage().doubleValue() < 0) error.add("收入额小于0,请检查!");
        if (personalItem.getDutyfreeamount().doubleValue() < 0) error.add("免税所得小于0,请检查!");
        if (personalItem.getPension().doubleValue() < 0) error.add("基本养老费小于0,请检查!");
        if (personalItem.getHealthinsurance().doubleValue() < 0) error.add("基本医保费小于0,请检查!");
        if (personalItem.getUnemploymentinsurance().doubleValue() < 0) error.add("失业保险费小于0,请检查!");
        if (personalItem.getHousingfund().doubleValue() < 0) error.add("住房公积金小于0,请检查!");
        if (personalItem.getOriginalproperty().doubleValue() < 0) error.add("财产原值小于0,请检查!");
        if (personalItem.getAllowdeduction().doubleValue() < 0) error.add("允许扣除的税费小于0,请检查!");
        if (personalItem.getOther().doubleValue() < 0) error.add("其他小于0,请检查!");
        if (personalItem.getAnnuity().doubleValue() < 0) error.add("年金小于0,请检查!");
        if (personalItem.getInsurance().doubleValue() < 0) error.add("商业健康险他小于0,请检查!");
        if (personalItem.getDeduction().doubleValue() < 0) error.add("投资抵扣小于0,请检查!");
        if (personalItem.getTotal().doubleValue() < 0) error.add("合计小于0,请检查!");
        if (personalItem.getDeductionamount().doubleValue() < 0) error.add("减除费用小于0,请检查!");
        if (personalItem.getDeductiondonate().doubleValue() < 0) error.add("准予扣除的捐赠额小于0,请检查!");
        if (personalItem.getTaxincome().doubleValue() < 0) error.add("应纳税所得额小于0,请检查!");
        if (personalItem.getTaxrate().doubleValue() < 0) error.add("税率小于0,请检查!");
        if (personalItem.getSpeeddeduction().doubleValue() < 0) error.add("速算扣除数小于0,请检查!");
        if (personalItem.getShouldpaytax().doubleValue() < 0) error.add("应纳税额小于0,请检查!");
        if (personalItem.getRelieftax().doubleValue() < 0) error.add("减免税额小于0,请检查!");
        if (personalItem.getShouldcosttax().doubleValue() < 0) error.add("应扣缴税额小于0,请检查!");
        if (personalItem.getAlreadycosttax().doubleValue() < 0) error.add("已扣缴税额小于0,请检查!");
        if (personalItem.getFinallytax().doubleValue() < 0) error.add("应补（退）税额小于0,请检查!");
        if (personalItem.getAlreadydeclarewage().doubleValue() < 0) error.add("已申报金额小于0,请检查!");
        if (personalItem.getCompanyamount().doubleValue() < 0) error.add("雇主负担金额小于0,请检查!");
        if (personalItem.getBegindate() == null || personalItem.getBegindate().equals("")) error.add("工资所属期起不能为空,请检查!");
        if (personalItem.getEnddate() == null || personalItem.getEnddate().equals("")) error.add("工资所属期止不能为空,请检查!");
        if (personalItem.getItemcode() == null || personalItem.getItemcode().equals("")) error.add("征税项目代码不能为空,请检查!");
        if (personalItem.getItemname() == null || personalItem.getItemname().equals("")) error.add("征税项目名称不能为空,请检查!");
        if (personalItem.getCountryid() == null || personalItem.getCountryid().equals("")) error.add("国家代码不能为空,请检查!");
        if (personalItem.getCountryname() == null || personalItem.getCountryname().equals(""))
            error.add("国家名称不能为空,请检查!");
        if (personalItem.getCardname() == null || personalItem.getCardname().equals("")) error.add("身份证件代码不能为空,请检查!");
        if (personalItem.getCardtype() == null || personalItem.getCardtype().equals("")) error.add("身份证件名称不能为空,请检查!");

        if (personalitemcodehash.size() <= 0) {
            List<BizMdDictionaryCode> BizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("incometype");
            BizMdDictionaryCodes.forEach(item -> {
                personalitemcodehash.put(item.getSn(), item.getName());
                personalitemcodehash.put(item.getName(), item.getSn());
            });
        }
        if (countryareahash.size() <= 0) {
            List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("countrytype");
            bizMdDictionaryCodes.forEach(item -> {
                countryareahash.put(item.getSn(), item.getName());
                countryareahash.put(item.getName(), item.getSn());
            });
        }
        if (cardclasshash.size() <= 0) {
            List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("cardtype");
            bizMdDictionaryCodes.forEach(item -> {
                cardclasshash.put(item.getSn(), item.getName());
                cardclasshash.put(item.getName(), item.getSn());
            });
        }
        //判断征税项目代码
        if (personalitemcodehash.get(personalItem.getItemname()) == null || personalitemcodehash.get(personalItem.getItemname()).equals(""))
            error.add("没有找到当前征税项目类型:" + "<<" + personalItem.getItemname() + ">>");
        if (personalitemcodehash.get(personalItem.getItemcode()) == null || personalitemcodehash.get(personalItem.getItemcode()).equals(""))
            error.add("没有找到当前征税项目类型:" + "<<" + personalItem.getItemcode() + ">>");

        //判断征国家代码
        if (countryareahash.get(personalItem.getCountryname()) == null || countryareahash.get(personalItem.getCountryname()).equals(""))
            error.add("没有找到当前征税项目类型:" + "<<" + personalItem.getCountryname() + ">>");
        if (countryareahash.get(personalItem.getCountryid()) == null || countryareahash.get(personalItem.getCountryid()).equals(""))
            error.add("没有找到当前征税项目类型:" + "<<" + personalItem.getCountryid() + ">>");

        //判断身份证件类型代码
        if (cardclasshash.get(personalItem.getCardname()) == null || cardclasshash.get(personalItem.getCardname()).equals(""))
            error.add("没有找到当前征税项目类型:" + "<<" + personalItem.getCardname() + ">>");
        if (cardclasshash.get(personalItem.getCardtype()) == null || cardclasshash.get(personalItem.getCardtype()).equals(""))
            error.add("没有找到当前征税项目类型:" + "<<" + personalItem.getCardtype() + ">>");
        //检查身份证
        if (personalItem.getCardtype().equals("201")) {
            checkCardNumber(personalItem.getName(), personalItem.getCardnumber(), error);
        }
        //检查数据
        validData(personalItem, error);

        //检查当前数据是否数据库中存在和以前年度是否申报过
        List<BizTaxInstanceCategoryPersonalPayrollItem> bizTaxInstanceCategoryPersonalPayrollItems = bizTaxInstanceCategoryPersonalPayrollItemService.findByCardnumberAndItemcode(personalItem.getCardnumber(), personalItem.getItemcode());
        if (bizTaxInstanceCategoryPersonalPayrollItems != null && bizTaxInstanceCategoryPersonalPayrollItems.size() > 0) {
            for( BizTaxInstanceCategoryPersonalPayrollItem item : bizTaxInstanceCategoryPersonalPayrollItems) {
                //判断当前人员是否同一个税种
                if (item.getBizTaxInstanceCategoryPersonalPayrollId() == personalItem.getBizTaxInstanceCategoryPersonalPayrollId() && item.getUseType().equals(BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare)) {
                    //判断是否已经存在
                    if (personalItem.getId() != item.getId()) {
                        bizTaxInstanceCategoryPersonalPayrollItemStateDTO.setCode(2);
                        bizTaxInstanceCategoryPersonalPayrollItemStateDTO.setId(item.getId());
                    }
                } else if (item.getBizTaxInstanceCategoryPersonalPayrollId() == personalItem.getBizTaxInstanceCategoryPersonalPayrollId() && item.getUseType().equals(BizTaxInstanceCategoryPersonalPayrollItem.UseType.result) && item.getBizTaxInstanceCategoryPersonalPayrollId() != personalItem.getBizTaxInstanceCategoryPersonalPayrollId()){
                    //判断所属期是否一致
                    if (item.getBegindate() != null && item.getBegindate().equals(personalItem.getBegindate()) && item.getEnddate() != null && item.getEnddate().equals(personalItem.getEnddate())) {
                        //校验当前人员的工资所属期是否在当前公司以前年度申报过
                        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = bizTaxInstanceCategoryPersonalPayrollService.findById(item.getBizTaxInstanceCategoryPersonalPayrollId());
                        if (bizTaxInstanceCategoryPersonalPayroll != null) {
                            BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(bizTaxInstanceCategoryPersonalPayroll.getBizTaxInstanceCategoryId());
                            if (bizTaxInstanceCategory != null && bizTaxInstanceCategory.getDeclareType() != null && (bizTaxInstanceCategory.getDeclareType().equals(DeclareType.yun9) || bizTaxInstanceCategory.getDeclareType().equals(DeclareType.taxOffice))) {
                                BizTaxInstance bizTaxInstance = bizTaxInstanceService.findById(bizTaxInstanceCategory.getBizTaxInstanceId());
                                //判断机构id和客户id是否一致
                                if (bizTaxInstance != null && bizTaxInstance.getMdCompanyId() == mdCompanyId && bizTaxInstance.getMdInstClientId() == mdInstClientId) {
                                    error.add(personalItem.getName() + ":以前年度已经申报过" + personalItem.getBegindate() + "至" + personalItem.getEnddate() + "的" + personalItem.getItemname());
                                }
                            }
                        }
                    }
                }
            }
        }
        //把所有校验错误push进去
        bizTaxInstanceCategoryPersonalPayrollItemStateDTO.setMessage(error);
        if (bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getMessage().size() > 0) {
            bizTaxInstanceCategoryPersonalPayrollItemStateDTO.setCode(1);
        }
        return bizTaxInstanceCategoryPersonalPayrollItemStateDTO;

    }

    @Override
    public BizTaxInstanceCategoryPersonalPayrollItem calculate(BizTaxInstanceCategoryPersonalPayrollItem perspnalItem) {
        //合计计算错误!合计 = 基本养老保险费 + 基本医疗保险费 + 失业保险费 + 住房公积金 + 财产原值 + 允许扣除的税费 + 年金 + 商业健康险 + 投资抵扣 + 其它扣除"
        BigDecimal total = toNumber(perspnalItem.getHealthinsurance())
                .add(toNumber(perspnalItem.getHousingfund()))
                .add(toNumber(perspnalItem.getAllowdeduction()))
                .add(toNumber(perspnalItem.getPension()))
                .add(toNumber(perspnalItem.getUnemploymentinsurance()))
                .add(toNumber(perspnalItem.getOriginalproperty()))
                .add(toNumber(perspnalItem.getOther()))
                .add(toNumber(perspnalItem.getInsurance()))
                .add(toNumber(perspnalItem.getAnnuity()))
                .add(toNumber(perspnalItem.getDeduction()));
        perspnalItem.setTotal(total);

        //减除费用计算错误!减除费用 = 4800 // 正常工资薪金
        if (perspnalItem.getItemcode().equals("0102")) {
            perspnalItem.setDeductionamount(new BigDecimal(4800));
        } else if (perspnalItem.getItemcode().equals("0101")) {
            perspnalItem.setDeductionamount(new BigDecimal(3500));
        } else if (perspnalItem.getItemcode().equals("0200")) {
            // 减除费用计算错误!减除费用 = 1600
            perspnalItem.setDeductionamount(new BigDecimal(1600));
        } else if (perspnalItem.getItemcode().equals("0400") || perspnalItem.getItemcode().equals("0500") || perspnalItem.getItemcode().equals("0600") || perspnalItem.getItemcode().equals("0800") || perspnalItem.getItemcode().equals("0801")) {
            double jg = (perspnalItem.getWage().doubleValue() - perspnalItem.getDutyfreeamount().doubleValue() - perspnalItem.getTotal().doubleValue());
            if (jg <= 4000) {
                // 减除费用计算错误!减除费用 = 800
                perspnalItem.setDeductionamount(new BigDecimal(800));
            } else {
                // 减除费用计算错误!减除费用 = (收入额 - 免税所得 - 合计) * 10000 * 0.2 / 10000
                perspnalItem.setDeductionamount(new BigDecimal((jg * 10000 * 0.2 / 10000)));
            }
        } else {
            perspnalItem.setDeductionamount(new BigDecimal(0));
        }
        //校验应纳税额所得额
        if (perspnalItem.getWage().doubleValue() - perspnalItem.getDutyfreeamount().doubleValue() - perspnalItem.getTotal().doubleValue() - perspnalItem.getDeductiondonate().doubleValue() - perspnalItem.getDeductionamount().doubleValue() > 0) {
            // 应纳税所得额计算错误!应纳税所得额 = 收入额 - 免税所得 - 合计 - 减除费用 - 准予扣除的捐赠额
            BigDecimal taxIncome = toNumber(perspnalItem.getWage())
                    .subtract(toNumber(perspnalItem.getDutyfreeamount()))
                    .subtract(toNumber(perspnalItem.getTotal()))
                    .subtract(toNumber(perspnalItem.getDeductiondonate()))
                    .subtract(toNumber(perspnalItem.getDeductionamount()));
            perspnalItem.setTaxincome(taxIncome);
        } else {
            perspnalItem.setTaxincome(new BigDecimal(0));
        }
        //校验税率和速算扣除数
        if (perspnalItem.getItemcode().equals("0101") || perspnalItem.getItemcode().equals("0109") || perspnalItem.getItemcode().equals("0107") || perspnalItem.getItemcode().equals("0108") || perspnalItem.getItemcode().equals("0102") || perspnalItem.getItemcode().equals("0110") || perspnalItem.getItemcode().equals("0111")) {
            if (perspnalItem.getTaxincome().doubleValue() >= 0 && perspnalItem.getTaxincome().doubleValue() <= 1500) {
                // 税率计算错误!税率 = 0.03
                perspnalItem.setTaxrate(new BigDecimal(0.03));
                perspnalItem.setSpeeddeduction(new BigDecimal(0.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 1500 && perspnalItem.getTaxincome().doubleValue() <= 4500) {
                perspnalItem.setTaxrate(new BigDecimal(0.10));
                // 速算扣除数计算错误!速算扣除数 = 105.00
                perspnalItem.setSpeeddeduction(new BigDecimal(105.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 4500 && perspnalItem.getTaxincome().doubleValue() <= 9000) {
                // 税率计算错误!税率 = 0.20
                perspnalItem.setTaxrate(new BigDecimal(0.20));
                perspnalItem.setSpeeddeduction(new BigDecimal(555.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 9000 && perspnalItem.getTaxincome().doubleValue() <= 35000) {
                perspnalItem.setTaxrate(new BigDecimal(0.25));
                // 速算扣除数计算错误!速算扣除数 = 1005.00
                perspnalItem.setSpeeddeduction(new BigDecimal(1005));
            } else if (perspnalItem.getTaxincome().doubleValue() > 35000 && perspnalItem.getTaxincome().doubleValue() <= 55000) {

                // 税率计算错误!税率 = 0.30
                perspnalItem.setTaxrate(new BigDecimal(0.30));
                perspnalItem.setSpeeddeduction(new BigDecimal(2755));
            } else if (perspnalItem.getTaxincome().doubleValue() > 55000 && perspnalItem.getTaxincome().doubleValue() <= 80000) {
                perspnalItem.setTaxrate(new BigDecimal(0.35));
                perspnalItem.setSpeeddeduction(new BigDecimal(5505));
            } else if (perspnalItem.getTaxincome().doubleValue() > 80000) {
                perspnalItem.setTaxrate(new BigDecimal(0.45));
                perspnalItem.setSpeeddeduction(new BigDecimal(13505.00));
            }
        } else if (perspnalItem.getItemcode().equals("0103")) {
            if (perspnalItem.getTaxincome().doubleValue() >= 0 && perspnalItem.getTaxincome().doubleValue() <= 18000) {
                perspnalItem.setTaxrate(new BigDecimal(0.03));
                perspnalItem.setSpeeddeduction(new BigDecimal(0.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 18000 && perspnalItem.getTaxincome().doubleValue() <= 54000) {
                perspnalItem.setTaxrate(new BigDecimal(0.10));
                perspnalItem.setSpeeddeduction(new BigDecimal(105.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 54000 && perspnalItem.getTaxincome().doubleValue() <= 108000) {
                perspnalItem.setTaxrate(new BigDecimal(0.20));
                perspnalItem.setSpeeddeduction(new BigDecimal(555.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 108000 && perspnalItem.getTaxincome().doubleValue() <= 420000) {
                perspnalItem.setTaxrate(new BigDecimal(0.25));
                perspnalItem.setSpeeddeduction(new BigDecimal(1005));
            } else if (perspnalItem.getTaxincome().doubleValue() > 420000 && perspnalItem.getTaxincome().doubleValue() <= 660000) {
                perspnalItem.setTaxrate(new BigDecimal(0.30));
                perspnalItem.setSpeeddeduction(new BigDecimal(2755));
            } else if (perspnalItem.getTaxincome().doubleValue() > 660000 && perspnalItem.getTaxincome().doubleValue() <= 960000) {
                perspnalItem.setTaxrate(new BigDecimal(0.35));
                perspnalItem.setSpeeddeduction(new BigDecimal(5505));
            } else if (perspnalItem.getTaxincome().doubleValue() > 960000) {
                perspnalItem.setTaxrate(new BigDecimal(0.45));
                perspnalItem.setSpeeddeduction(new BigDecimal(13505));
            }
        } else if (perspnalItem.getItemcode().equals("0400")) {
            if (perspnalItem.getTaxincome().doubleValue() >= 0 && perspnalItem.getTaxincome().doubleValue() <= 20000) {
                perspnalItem.setTaxrate(new BigDecimal(0.20));
                perspnalItem.setSpeeddeduction(new BigDecimal(0));
            } else if (perspnalItem.getTaxincome().doubleValue() > 20000 && perspnalItem.getTaxincome().doubleValue() <= 50000) {
                perspnalItem.setTaxrate(new BigDecimal(0.30));
                perspnalItem.setSpeeddeduction(new BigDecimal(2000.00));
            } else if (perspnalItem.getTaxincome().doubleValue() > 50000) {
                perspnalItem.setTaxrate(new BigDecimal(0.40));
                perspnalItem.setSpeeddeduction(new BigDecimal(7000.00));
            }
        } else if (perspnalItem.getItemcode().equals("0801") || perspnalItem.getItemcode().equals("9900") || perspnalItem.getItemcode().equals("0700") || perspnalItem.getItemcode().equals("0600") || perspnalItem.getItemcode().equals("0999") || perspnalItem.getItemcode().equals("0500") || perspnalItem.getItemcode().equals("0901") || perspnalItem.getItemcode().equals("0899") || perspnalItem.getItemcode().equals("0902") || perspnalItem.getItemcode().equals("0904") || perspnalItem.getItemcode().equals("0905") || perspnalItem.getItemcode().equals("1000")) {
            perspnalItem.setTaxrate(new BigDecimal(0.20));
            perspnalItem.setSpeeddeduction(new BigDecimal(0.00));
        } else {
            perspnalItem.setTaxrate(new BigDecimal(0));
            perspnalItem.setSpeeddeduction(new BigDecimal(0.00));
        }
        //校验应应纳税额
        if ((perspnalItem.getTaxincome().doubleValue() * perspnalItem.getTaxrate().doubleValue() - perspnalItem.getSpeeddeduction().doubleValue()) > 0) {
            // 应纳税额计算错误!应纳税额 = 应纳税所得额 * 税率 - 速算扣除数
            BigDecimal shouldPayTax = toNumber(perspnalItem.getTaxincome()).multiply(toNumber(perspnalItem.getTaxrate()))
                    .subtract(toNumber(perspnalItem.getSpeeddeduction()));
            perspnalItem.setShouldpaytax(shouldPayTax);
        } else {
            perspnalItem.setShouldpaytax(new BigDecimal(0));
        }
        //校验应扣缴税额
        if ((perspnalItem.getShouldpaytax().doubleValue() - perspnalItem.getRelieftax().doubleValue()) > 0) {
            //应扣缴税额计算错误!应扣缴税额 = 应纳税额 - 减免税额
            perspnalItem.setShouldcosttax(toNumber(perspnalItem.getShouldpaytax()).subtract(toNumber(perspnalItem.getRelieftax())));
        } else {
            perspnalItem.setShouldcosttax(new BigDecimal(0));
        }
        //校验应补退税额
        if ((perspnalItem.getShouldcosttax().doubleValue() - perspnalItem.getAlreadycosttax().doubleValue()) > 0) {
            // 应补（退）税额计算错误!应补（退）税额 = 应扣缴税额 - 已扣缴税额
            perspnalItem.setFinallytax(toNumber(perspnalItem.getShouldcosttax()).subtract(perspnalItem.getAlreadycosttax()));
        } else {
            perspnalItem.setFinallytax(new BigDecimal(0));
        }
        return perspnalItem;
    }

    private BigDecimal toNumber(BigDecimal target) {
        if (null ==  target) {
            return new BigDecimal("0.00");
        }
        return target;
    }

    private void checkCardNumber(String name, String idCard, List error) {
        /**
         * 身份证15位编码规则：dddddd yymmdd xx p  dddddd：地区码  yymmdd: 出生年月日  xx:
         * 顺序类编码，无法确定  p: 性别，奇数为男，偶数为女
         * <p /> 身份证18位编码规则：dddddd yyyymmdd xxx y  dddddd：地区码 yyyymmdd: 出生年月日
         * xxx:顺序类编码，无法确定，奇数为男，偶数为女 y: 校验码，该位数值可通过前17位计算获得
         * <p />  18位号码加权因子为(从右到左) Wi = [ 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5,
         * 8, 4, 2,1 ] 验证位 Y = [ 1, 0, 10, 9, 8, 7, 6, 5, 4, 3, 2 ] 校验位计算公式：Y_P =
         * mod( ∑(Ai×Wi),11 )  i为身份证号码从右往左数的 2...18 位; Y_P为脚丫校验码所在校验码数组位置
         */

        // 校验身份证号码
        String yyyy;
        String mm;
        String dd;
        String birthday;
        String address;
        String sex;
        // 是否输入
        String id = idCard;
        int id_length = id.length();
        if (id_length == 0) {
            error.add(name + "(" + idCard + ")" + ":身份证号码长度不能为零!");
        }
        // 长度校验
        if (id_length != 15 && id_length != 18) {
            error.add(name + "(" + idCard + ")" + ":身份证号码长度只能为15位或18位!");
        }
        // 地区校验
        JSONObject area = new JSONObject();
        area.put("11", "北京");
        area.put("12", "天津");
        area.put("13", "河北");
        area.put("14", "山西");
        area.put("15", "内蒙古");
        area.put("21", "辽宁");
        area.put("22", "吉林");
        area.put("23", "黑龙江");
        area.put("31", "上海");
        area.put("32", "江苏");
        area.put("33", "浙江");
        area.put("34", "安徽");
        area.put("35", "福建");
        area.put("36", "江西");
        area.put("37", "山东");
        area.put("41", "河南");
        area.put("42", "湖北");
        area.put("43", "湖南");
        area.put("44", "广东");
        area.put("45", "广西");
        area.put("46", "海南");
        area.put("50", "重庆");
        area.put("51", "四川");
        area.put("52", "贵州");
        area.put("53", "云南");
        area.put("54", "西藏");
        area.put("61", "陕西");
        area.put("62", "甘肃");
        area.put("63", "青海");
        area.put("64", "宁夏");
        area.put("65", "新疆");
        area.put("71", "台湾");
        area.put("81", "香港");
        area.put("82", "澳门");
        area.put("91", "国外");

        if (area.getString(idCard.toString().substring(0, 2)) == null) {
            error.add(name + "(" + idCard + ")" + ":身份证号码地区号码不正确!");
        } else
            address = area.getString(idCard.toString().substring(0, 2));
        // 日期校验
        if (id_length == 15) {

            String c = idCard.substring(14, 15).toUpperCase(); // 转为大写X
            if (c.equals("X")) {
                // 判断是否纯数字
                if (Double.isNaN(Double.parseDouble(idCard.substring(0, 14)))) {
                    error.add(name + "(" + idCard + ")" + ":身份证号码前14位不是纯数字!");
                }
            } else {
                // 判断是否纯数字
                if (Double.isNaN(Double.parseDouble(idCard))) {
                    error.add(name + "(" + idCard + ")" + ":身份证号码不是纯数字!");
                }
            }

            yyyy = "19" + id.toString().substring(6, 8);
            mm = id.toString().substring(8, 10);
            dd = id.toString().substring(10, 12);

            if (Double.parseDouble(mm) > 12 || Double.parseDouble(mm) <= 0) {
                error.add(name + "(" + idCard + ")" + ":身份证号码出生月份超出范围(1~12)!");
            }

            if (Double.parseDouble(dd) > 31 || Double.parseDouble(dd) <= 0) {
                error.add(name + "(" + idCard + ")" + ":身份证号码出生日期超出范围(1~31)!");
            }

            birthday = yyyy + "-" + mm + "-" + dd;

            if ("13579".indexOf(id.toString().substring(14, 15)) != -1) {
                sex = "女";
            } else {
                sex = "男";
            }
        } else if (id_length == 18) {
            String c = idCard.substring(17).toUpperCase(); // 转为大写X
            if (c.equals("X")) {
                // 判断是否纯数字
                if (Double.isNaN(Double.parseDouble(idCard.substring(0, 17)))) {
                    error.add(name + "(" + idCard + ")" + ":身份证号码前17位不是纯数字!");
                }
            } else {
                // 判断是否纯数字
                if (Double.isNaN(Double.parseDouble(idCard))) {
                    error.add(name + "(" + idCard + ")" + ":身份证号码前17位不是纯数字!");
                }
            }

            yyyy = id.toString().substring(6, 10);
            if (Double.parseDouble(yyyy) > 2200 || Double.parseDouble(yyyy) < 1900) {
                error.add(name + "(" + idCard + ")" + ":身份证号码出生年份超出范围!");
            }

            mm = id.toString().substring(10, 12);
            if (Double.parseDouble(mm) > 12 || Double.parseDouble(mm) <= 0) {
                error.add(name + "(" + idCard + ")" + ":身份证号码出生月份超出范围(1~12)!");
            }

            dd = id.toString().substring(12, 14);
            if (Double.parseDouble(dd) > 31 || Double.parseDouble(dd) <= 0) {
                error.add(name + "(" + idCard + ")" + ":身份证号码出生日期超出范围(1~31)!");
            }
            // 校验位校验
            if (!isChinaIDCard(idCard, error)) {
                error.add(name + "(" + idCard + ")" + ":身份证号码校验位有误!");
            }
            birthday = yyyy + "-" + mm + "-" + dd;
            //alert(birthday);
            if ("13579".indexOf(id.toString().substring(14, 15)) != -1) {
                sex = "女";
            } else {
                sex = "男";
            }

        }
    }

    private boolean isChinaIDCard(String StrNo, List error) {

        StrNo = StrNo.toString();
        double a, b;
        String c;
        a = Double.parseDouble(StrNo.substring(0, 1)) * 7 + Double.parseDouble(StrNo.substring(1, 2)) * 9 +
                Double.parseDouble(StrNo.substring(2, 3)) * 10;
        a = a + Double.parseDouble(StrNo.substring(3, 4)) * 5 + Double.parseDouble(StrNo.substring(4, 5)) * 8 +
                Double.parseDouble(StrNo.substring(5, 6)) * 4;
        a = a + Double.parseDouble(StrNo.substring(6, 7)) * 2 + Double.parseDouble(StrNo.substring(7, 8)) * 1 +
                Double.parseDouble(StrNo.substring(8, 9)) * 6;
        a = a + Double.parseDouble(StrNo.substring(9, 10)) * 3 + Double.parseDouble(StrNo.substring(10, 11)) *
                7 + Double.parseDouble(StrNo.substring(11, 12)) * 9;
        a = a + Double.parseDouble(StrNo.substring(12, 13)) * 10 + Double.parseDouble(StrNo.substring(13, 14)) *
                5 + Double.parseDouble(StrNo.substring(14, 15)) * 8;
        a = a + Double.parseDouble(StrNo.substring(15, 16)) * 4 + Double.parseDouble(StrNo.substring(16, 17)) *
                2;
        b = a % 11;

        if (b == 2)
        // 最后一位为校验位
        {
            c = StrNo.substring(17).toUpperCase(); // 转为大写X
        } else {
            c = StrNo.substring(17);
        }

        switch ((int) b) {
            case 0:
                if (!c.equals("1")) {
                    return false;
                }
                break;
            case 1:
                if (!c.equals("0")) {
                    return false;
                }
                break;
            case 2:
                if (!c.equals("X")) {
                    return false;
                }
                break;
            case 3:
                if (!c.equals("9")) {
                    return false;
                }
                break;
            case 4:
                if (!c.equals("8")) {
                    return false;
                }
                break;
            case 5:
                if (!c.equals("7")) {
                    return false;
                }
                break;
            case 6:
                if (!c.equals("6")) {
                    return false;
                }
                break;
            case 7:
                if (!c.equals("5")) {
                    return false;
                }
                break;
            case 8:
                if (!c.equals("4")) {
                    return false;
                }
                break;
            case 9:
                if (!c.equals("3")) {
                    return false;
                }
                break;
            case 10:
                if (!c.equals("2")) {
                    return false;
                }
        }
        return true;
    }

    private void validData(BizTaxInstanceCategoryPersonalPayrollItem perspnalItem, List error) {
        //校验合计
        if (Math.abs(perspnalItem.getTotal().doubleValue() - perspnalItem.getHealthinsurance().doubleValue() - perspnalItem.getHousingfund().doubleValue() - perspnalItem.getAllowdeduction().doubleValue() - perspnalItem.getPension().doubleValue() - perspnalItem.getUnemploymentinsurance().doubleValue() - perspnalItem.getOriginalproperty().doubleValue() - (perspnalItem.getOther().doubleValue()) - perspnalItem.getInsurance().doubleValue() - perspnalItem.getAnnuity().doubleValue() - perspnalItem.getDeduction().doubleValue()) > 0.1) {
            error.add("合计计算错误!合计 = 基本养老保险费 + 基本医疗保险费 + 失业保险费 + 住房公积金 + 财产原值 + 允许扣除的税费 + 年金 + 商业健康险 + 投资抵扣 + 其它扣除");
        }
        //校验减除费用
        if (perspnalItem.getItemcode().equals("0102")) { // 正常工资薪金
            if (perspnalItem.getDeductionamount().doubleValue() != 4800) {
                error.add("减除费用计算错误!减除费用 = 4800");
            }
        } else if (perspnalItem.getItemcode().equals("0101")) {
            if (perspnalItem.getDeductionamount().doubleValue() != 3500) {
                error.add("减除费用计算错误!减除费用 = 3500");
            }
        } else if (perspnalItem.getItemcode().equals("0200")) {
            if (perspnalItem.getDeductionamount().doubleValue() != 1600) {
                error.add("减除费用计算错误!减除费用 = 1600");
            }
        } else if (perspnalItem.getItemcode().equals("0400") || perspnalItem.getItemcode().equals("0500") || perspnalItem.getItemcode().equals("0600") || perspnalItem.getItemcode().equals("0800") || perspnalItem.getItemcode().equals("0801")) {
            double jg = (perspnalItem.getWage().doubleValue() - perspnalItem.getDutyfreeamount().doubleValue() - perspnalItem.getTotal().doubleValue());
            if (jg <= 4000) {
                if (perspnalItem.getDeductionamount().doubleValue() != 800) {
                    error.add("减除费用计算错误!减除费用 = 800");
                }
            } else {
                if (Math.abs(perspnalItem.getDeductionamount().doubleValue() - (jg * 10000 * 0.2 / 10000)) > 0.1) {
                    error.add("减除费用计算错误!减除费用 = (收入额 - 免税所得 - 合计) * 10000 * 0.2 / 10000");
                }
            }
        } else {
            if (perspnalItem.getDeductionamount().doubleValue() != 0.00) {
                error.add("减除费用计算错误!减除费用 = 0");
            }
        }
        //校验应纳税额所得额
        if (perspnalItem.getWage().doubleValue() - perspnalItem.getDutyfreeamount().doubleValue() - perspnalItem.getTotal().doubleValue() - perspnalItem.getDeductiondonate().doubleValue() - perspnalItem.getDeductionamount().doubleValue() > 0) {
            if (Math.abs((perspnalItem.getTaxincome().doubleValue()) - (perspnalItem.getWage().doubleValue() - perspnalItem.getDutyfreeamount().doubleValue() - perspnalItem.getTotal().doubleValue() - perspnalItem.getDeductiondonate().doubleValue() - perspnalItem.getDeductionamount().doubleValue())) > 0.1) {
                error.add("应纳税所得额计算错误!应纳税所得额 = 收入额 - 免税所得 - 合计 - 减除费用 - 准予扣除的捐赠额");
            }
        } else {
            if (perspnalItem.getTaxincome().doubleValue() != 0) {
                error.add("应纳税所得额计算错误!应纳税所得额 = 0");
            }
        }
        //校验税率和速算扣除数
        if (perspnalItem.getItemcode().equals("0101") || perspnalItem.getItemcode().equals("0109") || perspnalItem.getItemcode().equals("0107") || perspnalItem.getItemcode().equals("0108") || perspnalItem.getItemcode().equals("0102") || perspnalItem.getItemcode().equals("0110") || perspnalItem.getItemcode().equals("0111")) {
            if (perspnalItem.getTaxincome().doubleValue() >= 0 && perspnalItem.getTaxincome().doubleValue() <= 1500) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.03) {
                    error.add("税率计算错误!税率 = 0.03");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 0.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 0");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 1500 && perspnalItem.getTaxincome().doubleValue() <= 4500) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.10) {
                    error.add("税率计算错误!税率 = 0.10");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 105.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 105.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 4500 && perspnalItem.getTaxincome().doubleValue() <= 9000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.20) {
                    error.add("税率计算错误!税率 = 0.20");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 555.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 555.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 9000 && perspnalItem.getTaxincome().doubleValue() <= 35000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.25) {
                    error.add("税率计算错误!税率 = 0.25");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 1005) {
                    error.add("速算扣除数计算错误!速算扣除数 = 1005.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 35000 && perspnalItem.getTaxincome().doubleValue() <= 55000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.30) {
                    error.add("税率计算错误!税率 = 0.30");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 2755) {
                    error.add("速算扣除数计算错误!速算扣除数 = 2755.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 55000 && perspnalItem.getTaxincome().doubleValue() <= 80000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.35) {
                    error.add("税率计算错误!税率 = 0.35");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 5505) {
                    error.add("速算扣除数计算错误!速算扣除数 = 5505.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 80000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.45) {
                    error.add("税率计算错误!税率 = 0.45");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 13505) {
                    error.add("速算扣除数计算错误!速算扣除数 = 13505.00");
                }
            }
        } else if (perspnalItem.getItemcode().equals("0103")) {
            if (perspnalItem.getTaxincome().doubleValue() >= 0 && perspnalItem.getTaxincome().doubleValue() <= 18000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.03) {
                    error.add("税率计算错误!税率 = 0.03");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 0.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 0.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 18000 && perspnalItem.getTaxincome().doubleValue() <= 54000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.10) {
                    error.add("税率计算错误!税率 = 0.10");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 105.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 105.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 54000 && perspnalItem.getTaxincome().doubleValue() <= 108000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.20) {
                    error.add("税率计算错误!税率 = 0.20");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 555.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 555.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 108000 && perspnalItem.getTaxincome().doubleValue() <= 420000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.25) {
                    error.add("税率计算错误!税率 = 0.25");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 1005) {
                    error.add("速算扣除数计算错误!速算扣除数 = 1005.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 420000 && perspnalItem.getTaxincome().doubleValue() <= 660000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.30) {
                    error.add("税率计算错误!税率 = 0.30");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 2755) {
                    error.add("速算扣除数计算错误!速算扣除数 = 2755.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 660000 && perspnalItem.getTaxincome().doubleValue() <= 960000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.35) {
                    error.add("税率计算错误!税率 = 0.35");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 5505) {
                    error.add("速算扣除数计算错误!速算扣除数 = 5505.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 960000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.45) {
                    error.add("税率计算错误!税率 = 0.45");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 13505) {
                    error.add("速算扣除数计算错误!速算扣除数 = 13505.00");
                }
            }
        } else if (perspnalItem.getItemcode().equals("0400")) {
            if (perspnalItem.getTaxincome().doubleValue() >= 0 && perspnalItem.getTaxincome().doubleValue() <= 20000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.20) {
                    error.add("税率计算错误!税率 = 0.20");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 0.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 0.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 20000 && perspnalItem.getTaxincome().doubleValue() <= 50000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.30) {
                    error.add("税率计算错误!税率 = 0.30");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 2000.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 2000.00");
                }
            } else if (perspnalItem.getTaxincome().doubleValue() > 50000) {
                if (perspnalItem.getTaxrate().doubleValue() != 0.40) {
                    error.add("税率计算错误!税率 = 0.40");
                }
                if (perspnalItem.getSpeeddeduction().doubleValue() != 7000.00) {
                    error.add("速算扣除数计算错误!速算扣除数 = 7000.00");
                }
            }
        } else if (perspnalItem.getItemcode().equals("0801") || perspnalItem.getItemcode().equals("9900") || perspnalItem.getItemcode().equals("0700") || perspnalItem.getItemcode().equals("0600") || perspnalItem.getItemcode().equals("0999") || perspnalItem.getItemcode().equals("0500") || perspnalItem.getItemcode().equals("0901") || perspnalItem.getItemcode().equals("0899") || perspnalItem.getItemcode().equals("0902") || perspnalItem.getItemcode().equals("0904") || perspnalItem.getItemcode().equals("0905") || perspnalItem.getItemcode().equals("1000")) {
            if (perspnalItem.getTaxrate().doubleValue() != 0.20) {
                error.add("税率计算错误!税率 = 0.20");
            }
            if (perspnalItem.getSpeeddeduction().doubleValue() != 0.00) {
                error.add("速算扣除数计算错误!速算扣除数 = 0.00");
            }
        } else {
            if (perspnalItem.getTaxrate().doubleValue() != 0.00) {
                error.add("税率计算错误!税率 = 0.00");
            }
            if (perspnalItem.getSpeeddeduction().doubleValue() != 0.00) {
                error.add("速算扣除数计算错误!速算扣除数 = 0.00");
            }
        }
        //校验应应纳税额
        if ((perspnalItem.getTaxincome().doubleValue() * perspnalItem.getTaxrate().doubleValue() - perspnalItem.getSpeeddeduction().doubleValue()) > 0) {
            if (Math.abs(perspnalItem.getShouldpaytax().doubleValue() - (perspnalItem.getTaxincome().doubleValue() * perspnalItem.getTaxrate().doubleValue() - perspnalItem.getSpeeddeduction().doubleValue())) > 0.1) {
                error.add("应纳税额计算错误!应纳税额 = 应纳税所得额 * 税率 - 速算扣除数");
            }
        } else {
            if (perspnalItem.getShouldpaytax().doubleValue() != 0) {
                error.add("应纳税额计算错误!应纳税额 = 0");
            }
        }
        //校验应扣缴税额
        if ((perspnalItem.getShouldpaytax().doubleValue() - perspnalItem.getRelieftax().doubleValue()) > 0) {
            if (Math.abs(perspnalItem.getShouldcosttax().doubleValue() - (perspnalItem.getShouldpaytax().doubleValue() - perspnalItem.getRelieftax().doubleValue())) > 0.1) {
                error.add("应扣缴税额计算错误!应扣缴税额 = 应纳税额 - 减免税额");
            }
        } else {
            if (perspnalItem.getShouldcosttax().doubleValue() != 0) {
                error.add("应扣缴税额计算错误!应扣缴税额 = 0");
            }
        }
        //校验应补退税额
        if ((perspnalItem.getShouldcosttax().doubleValue() - perspnalItem.getAlreadycosttax().doubleValue()) > 0) {
            if (Math.abs(perspnalItem.getFinallytax().doubleValue() - (perspnalItem.getShouldcosttax().doubleValue() - perspnalItem.getAlreadycosttax().doubleValue())) > 0.1) {
                error.add("应补（退）税额计算错误!应补（退）税额 = 应扣缴税额 - 已扣缴税额");
            }
        } else {
            if (perspnalItem.getFinallytax().doubleValue() != 0) {
                error.add("应补（退）税额计算错误!应补（退）税额 = 0");
            }
        }
    }

    @Override
    public BizTaxInstanceCategoryPersonalPayrollItem initialize(BizTaxInstanceCategoryPersonalPayrollItem personalItem, long mdInstClientId, long mdCompanyId, long mdAccountCycleId, long md_area_id) {

        //查询征税项目控制编码
        if ((personalItem.getItemcode() == null || personalItem.getItemcode().equals("")) && (personalItem.getItemname() == null || personalItem.getItemname().equals(""))) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "征税项目代码和征税项目名称不能同时为空");
        } else {
            //如果缺的是征税项目代码
            if (personalItem.getItemcode() == null || personalItem.getItemcode().equals("")) {
                if (personalitemcodehash.get(personalItem.getItemname()) == null) {
                    List<BizMdDictionaryCode> BizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("incometype");
                    BizMdDictionaryCodes.forEach(item -> {
                        personalitemcodehash.put(item.getSn(), item.getName());
                        personalitemcodehash.put(item.getName(), item.getSn());
                    });
                }
                if (personalitemcodehash.get(personalItem.getItemname()).equals("") || personalitemcodehash.get(personalItem.getItemname()) == null)
                    throw new BizException("没有找到当前征税项目类型：" + personalItem.getItemname());
                //设置Itemcode的值
                personalItem.setItemcode(personalitemcodehash.get(personalItem.getItemname()));
            } else {
                //如果缺的是征税项目名称
                if (personalitemcodehash.get(personalItem.getItemcode()) == null) {
                    List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("incometype");
                    bizMdDictionaryCodes.forEach(item -> {
                        personalitemcodehash.put(item.getSn(), item.getName());
                        personalitemcodehash.put(item.getName(), item.getSn());
                    });
                }
                if (personalitemcodehash.get(personalItem.getItemcode()).equals("") || personalitemcodehash.get(personalItem.getItemcode()) == null)
                    throw new BizException("没有找到当前征税项目名称：" + personalItem.getItemcode());
                //设置Itemname的值
                personalItem.setItemname(personalitemcodehash.get(personalItem.getItemcode()));
            }
        }

        //判断征税项目代码
        if ((personalItem.getCountryid() == null || personalItem.getCountryid().equals("")) && (personalItem.getCountryname() == null || personalItem.getCountryname().equals(""))) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "国家代码和国家名称不能同时为空");
        } else {
            //如果缺的是国家代码
            if (personalItem.getCountryid() == null || personalItem.getCountryid().equals("")) {
                if (countryareahash.get(personalItem.getCountryname()) == null) {
                    List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("countrytype");
                    bizMdDictionaryCodes.forEach(item -> {
                        countryareahash.put(item.getSn(), item.getName());
                        countryareahash.put(item.getName(), item.getSn());
                    });
                }
                if (countryareahash.get(personalItem.getCountryname()).equals("") || countryareahash.get(personalItem.getCountryname()) == null)
                    throw new BizException("没有找到当前国家类型：" + personalItem.getCountryname());
                //设置Countryid的值
                personalItem.setCountryid(countryareahash.get(personalItem.getCountryname()));
            } else {
                //如果缺的是国家名称
                if (countryareahash.get(personalItem.getCountryid()) == null) {
                    List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("countrytype");
                    bizMdDictionaryCodes.forEach(item -> {
                        countryareahash.put(item.getSn(), item.getName());
                        countryareahash.put(item.getName(), item.getSn());
                    });
                }
                if (countryareahash.get(personalItem.getCountryid()).equals("") || countryareahash.get(personalItem.getCountryid()) == null)
                    throw new BizException("没有找到当前国家名称：" + personalItem.getCountryid());
                //设置Countryname的值
                personalItem.setCountryname(countryareahash.get(personalItem.getCountryid()));
            }
        }

        //查询身份证件类型控制编码
        if ((personalItem.getCardtype() == null || personalItem.getCardtype().equals("")) && (personalItem.getCardname() == null || personalItem.getCardname().equals(""))) {
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "身份证件类型代码和身份证件类型名称不能同时为空");
        } else {
            if (personalItem.getCardtype() == null || personalItem.getCardtype().equals("")) {
                if (cardclasshash.get(personalItem.getCardname()) == null) {
                    List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("cardtype");
                    bizMdDictionaryCodes.forEach(item -> {
                        cardclasshash.put(item.getSn(), item.getName());
                        cardclasshash.put(item.getName(), item.getSn());
                    });
                }
                if (cardclasshash.get(personalItem.getCardname()).equals("") || cardclasshash.get(personalItem.getCardname()) == null)
                    throw new BizException("没有找到当前身份证件类型：" + personalItem.getCardname());
                //设置Cardtype的值
                personalItem.setCardtype(cardclasshash.get(personalItem.getCardname()));
            } else {
                if (cardclasshash.get(personalItem.getCardtype()) == null) {
                    List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("cardtype");
                    bizMdDictionaryCodes.forEach(item -> {
                        cardclasshash.put(item.getSn(), item.getName());
                        cardclasshash.put(item.getName(), item.getSn());
                    });
                }
                if (cardclasshash.get(personalItem.getCardtype()).equals("") || cardclasshash.get(personalItem.getCardtype()) == null)
                    throw new BizException("没有找到当前身份证件名称：" + personalItem.getCardtype());
                //设置Cardname的值
                personalItem.setCardname(cardclasshash.get(personalItem.getCardtype()));
            }
        }

        if ((personalItem.getBegindate() == null || personalItem.getBegindate().equals("")) && (personalItem.getEnddate() == null || personalItem.getEnddate().equals(""))) {
            BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findById(mdAccountCycleId);
            if (bizMdAccountCycle == null) throw BizTaxException.build(BizTaxException.Codes.DateError, "无法获取会计期间");
            List<BizMdAccountCycle> bizMdAccountCycles = bizMdAccountCycleService.findBySnAndType(bizMdAccountCycle.getSn(), CycleType.m, 1, 0);
            if (bizMdAccountCycles != null && bizMdAccountCycles.size() <= 0)
                throw BizTaxException.build(BizTaxException.Codes.DateError, "获取上月的会计期间出错");
            BizMdArea bizMdArea = bizMdAreaService.findById(md_area_id);
            if (bizMdArea == null) throw BizTaxException.build(BizTaxException.Codes.DateError, "获取税区出错");
            //查询税区
            if (bizMdArea.getSn().equals("shenzhen")) {
                //设置工资所属期起
                personalItem.setBegindate(new SimpleDateFormat("yyyy-MM-dd").format(bizMdAccountCycles.get(1).getBeginDate() * 1000));
                //设置工资所属期止
                personalItem.setEnddate(new SimpleDateFormat("yyyy-MM-dd").format(bizMdAccountCycles.get(1).getEndDate() * 1000));
            } else if (bizMdArea.getSn().equals("guangdong")) {
                //先查询上一个月的申报记录的人员清单
                BizTaxInstance bizTaxInstance = bizTaxInstanceService.findByMdInstClientIdAndMdCompanyIdAndMdAccountCycleIdAndTaxOfficeAndDisabled(mdInstClientId, mdCompanyId, bizMdAccountCycles.get(0).getId(), com.yun9.biz.tax.enums.TaxOffice.ds);
                if (bizTaxInstance != null) {
                    BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findBySn(TaxSn.m_personal_payroll);
                    if (bizTaxMdCategory != null) {
                        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findTaxCategoryIdAndInstanceId(bizTaxMdCategory.getId(), bizTaxInstance.getId());
                        if (bizTaxInstanceCategory != null && (bizTaxInstanceCategory.getDeclareType().equals(DeclareType.yun9) || bizTaxInstanceCategory.getDeclareType().equals(DeclareType.taxOffice))) {
                            BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategory.getId());
                            if (bizTaxInstanceCategoryPersonalPayroll != null) {
                                List<BizTaxInstanceCategoryPersonalPayrollItem> bizTaxInstanceCategoryPersonalPayrollItems = bizTaxInstanceCategoryPersonalPayrollItemService.findByBizTaxInstanceCategoryPersonalPayrollIdAndUseType(bizTaxInstanceCategoryPersonalPayroll.getId(), BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare);
                                if (bizTaxInstanceCategoryPersonalPayrollItems != null && bizTaxInstanceCategoryPersonalPayrollItems.size() > 0) {
                                    bizTaxInstanceCategoryPersonalPayrollItems.forEach(item -> {
                                        if (item.getName().equals(personalItem.getName()) && item.getCardnumber().equals(personalItem.getCardnumber()) && item.getItemcode().equals(personalItem.getItemcode())) {
                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                            Date startDate = java.sql.Date.valueOf(item.getBegindate());
                                            Date endDate = java.sql.Date.valueOf(item.getEnddate());
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.setTime(startDate);
                                            calendar.add(Calendar.MONTH, 1);
                                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                                            //取下月的时间
                                            personalItem.setBegindate(simpleDateFormat.format(calendar.getTime()));
                                            calendar.setTime(endDate);
                                            calendar.add(Calendar.MONTH, 1);
                                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                                            personalItem.setEnddate(simpleDateFormat.format(calendar.getTime()));
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            } else {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "暂不支持的区域:" + bizMdArea.getSn());
            }
        }
        return personalItem;

    }

    @Override
    public void bacthCreate(long mdInstClientId, long mdCompanyId, long mdAccountCycleId, long mdAreaId, long instanceCategoryPersonalPayrollId, List<BizTaxInstanceCategoryPersonalPayrollItem> itemList) {

        //获取会计期间
        BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(bizMdAccountCycleService.findById(mdAccountCycleId)).orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "会计期间不存在"));

        //调用初始化
        itemList.stream().forEach(item -> {
            item.setBizTaxInstanceCategoryPersonalPayrollId(instanceCategoryPersonalPayrollId);
            item.setUseType(BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare);
            initialize(item, mdInstClientId, mdCompanyId, mdAccountCycleId, mdAreaId);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = java.sql.Date.valueOf(DateUtils.longToDateString(bizMdAccountCycle.getBeginDate() * 1000, DateUtils.ZH_PATTERN_DAY));
            Date endDate = java.sql.Date.valueOf(DateUtils.longToDateString(bizMdAccountCycle.getEndDate() * 1000, DateUtils.ZH_PATTERN_DAY));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
            //取下月的时间

            calendar.setTime(endDate);
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

            if (StringUtils.isEmpty(item.getBegindate())) {
                item.setBegindate(simpleDateFormat.format(calendar.getTime()));
            }
            if (StringUtils.isEmpty(item.getEnddate())) {
                item.setEnddate(simpleDateFormat.format(calendar.getTime()));
            }
        });

        BizTaxInstanceCategoryPersonalPayrollItemStateDTO backDto = taxInstanceCategoryPersonalMFactory.addPersonalPayrollItem(itemList, instanceCategoryPersonalPayrollId, true);
        if (backDto.getCode() != 200) {
            throw BizTaxException.build(BizTaxException.Codes.task_callback_failed, backDto.getMessage());
        }
    }
}