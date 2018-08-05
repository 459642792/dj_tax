package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.opencsv.CSVReader;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.BizMdAreaService;
import com.yun9.biz.md.BizMdDictionaryCodeService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdDictionaryCode;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalMFactory;
import com.yun9.service.tax.core.dto.*;
import com.yun9.service.tax.core.enums.DataOperationType;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.utils.FileParse;
import com.yun9.service.tax.core.utils.FileUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


/**
 * @Author: chenbin
 * @Date: 2018-05-31
 * @Time: 16:02
 * @Description:
 */
@Component
public class TaxInstanceCategoryPersonalMFactoryImpl implements
    TaxInstanceCategoryPersonalMFactory {

    public static final Logger logger = LoggerFactory
        .getLogger(TaxInstanceCategoryPersonalMFactoryImpl.class);

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    TaxInstanceCategoryPersonalPayrollItemFactoryImpl taxInstanceCategoryPersonalPayrollItemFactory;

    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

    @Autowired
    private BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    private BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizMdAreaService bizMdAreaService;

    @Autowired
    BizMdDictionaryCodeService bizMdDictionaryCodeService;

    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;
   //日期正则
   private final String regex = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)";
    @Override
    public BizTaxPersonalImportDTO parsePersonalExcel(
        BizTaxPersonalImportSheetDTO bizTaxPersonalImportSheetDTO) {
        logger.debug("解析个税工资薪金文件开始");

        if (null == bizTaxPersonalImportSheetDTO.getFileData() || ""
            .equals(bizTaxPersonalImportSheetDTO.getFileData())) {
            throw BizTaxException.build(BizTaxException.Codes.Biz_Vat_Import_File_Error);
        }

        BizTaxInstance bizTaxInstance = bizTaxInstanceService
            .findByTaxId(bizTaxPersonalImportSheetDTO.getCategoryId(), TaxSn.m_personal_payroll);
        bizTaxPersonalImportSheetDTO.setTaxareaId(bizTaxInstance.getMdAreaId());
        bizTaxPersonalImportSheetDTO.setMdAccountCycleId(bizTaxInstance.getMdAccountCycleId());
        bizTaxPersonalImportSheetDTO.setMdCompanyId(bizTaxInstance.getMdCompanyId());
        bizTaxPersonalImportSheetDTO.setMdInstClientId(bizTaxInstance.getMdInstClientId());

        BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(
            bizMdAccountCycleService.findById(bizTaxPersonalImportSheetDTO.getMdAccountCycleId()))
            .orElseThrow(() -> BizTaxException
                .throwException(BizTaxException.Codes.TAX_VAT_CYCLE_ERRORS, "申报月份不存在"));

        String str = FileUtil.importFile(bizTaxPersonalImportSheetDTO.getFileUploadPath(),
            bizTaxPersonalImportSheetDTO.getFileData(),
            bizTaxPersonalImportSheetDTO.getFileOriginalName());
        File file = new File(str);

        List<BizTaxPersonalPayrollItem> sheetData = parsePersonal(file,
            bizTaxPersonalImportSheetDTO, bizMdAccountCycle.getSn());
        BizTaxPersonalImportDTO importDTO = this.separateItem(sheetData);
        //BizTaxPersonalImportDTO bizTaxPersonalImportDTO = new BizTaxPersonalImportDTO();
        //bizTaxPersonalImportDTO.setSingleSheet(sheetData);
        //private BizTaxPersonalImportDTO separateItem(List<BizTaxPersonalPayrollItem> itemList){
       
        return importDTO;
    }

    /**
     * 个人所得税解析数据
     */
    public List<BizTaxPersonalPayrollItem> parsePersonal(File file,
        BizTaxPersonalImportSheetDTO importSheetDTO, String sn) {
        logger.debug("开始解析文件：{}", file.getName());
        String fileType = file.getName().substring(file.getName().lastIndexOf("."));
        String type = StringUtils.isNotEmpty(fileType.substring(1)) ? fileType.substring(1) : null;
        logger.debug("文件名:{},文件类型:{}", file.getName(), type);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if ("csv".equals(type)) {
                return readCSVPersonal(inputStream, importSheetDTO, sn);
            } else {
                return parseExcelPersonal(type, inputStream, importSheetDTO, sn);
            }
        } catch (FileNotFoundException e) {
            logger.error("解析文件出现错误.", e);
            file.delete();
            throw BizTaxException
                .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION, "支持xlsx,xls,csv");
        } finally {
            logger.debug("关闭文件输入流");
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<BizTaxPersonalPayrollItem> buildPersonalSheet(Sheet sheet,
        BizTaxPersonalImportSheetDTO importSheetDTO, String sn) {

        int maxRow = sheet.getLastRowNum() + 1;

        logger.debug("解析Excel具体Sheet:{}，sheet共有{}行", sheet.getSheetName(), maxRow);
        List<BizTaxPersonalPayrollItem> bizTaxPersonalPayrollItems = new ArrayList<>();

        BizTaxInstanceCategoryPersonalPayrollItemStateDTO bizTaxInstanceCategoryPersonalPayrollItemStateDTO;
        List<String> message;
        for (int i = 0; i < maxRow; i++) {
            Row row = sheet.getRow(i);
            if (null == row) {
                break;
            }
            if (i == 0) {
                continue;
            }
            if (i == 1) {
                if (row.getCell(0).toString().contains("纳税人名称") && row.getCell(1).toString()
                    .contains("身份证照类型")) {
                    continue;
                } else {
                    throw BizTaxException
                        .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION,
                            "请按照下载的模板提交");
                }
            } else {
                BizTaxPersonalPayrollItem bizTaxPersonalPayrollItem = new BizTaxPersonalPayrollItem();
                BizTaxInstanceCategoryPersonalPayrollItem personalPayrollItem = new BizTaxInstanceCategoryPersonalPayrollItem();

                if (StringUtils.isEmpty(row.getCell(0)) && StringUtils.isEmpty(row.getCell(1))
                    && StringUtils.isEmpty(row.getCell(2)) && StringUtils.isEmpty(row.getCell(3))
                    && StringUtils.isEmpty(row.getCell(4)) && StringUtils.isEmpty(row.getCell(7))) {
                    continue;
                }

                setFORMULAValue(row);
                message = new ArrayList<>();
                int code = 0;
                personalPayrollItem.setSort(i + 1);
                personalPayrollItem.setName(StringUtils.isEmpty(row.getCell(0))?"":replaceSpace(row.getCell(0).toString()));
                personalPayrollItem.setCountryname(StringUtils.isEmpty(row.getCell(3))?"":replaceSpace(row.getCell(3).toString()));
                personalPayrollItem.setCardname(StringUtils.isEmpty(row.getCell(1))?"":replaceSpace(row.getCell(1).toString()));
                personalPayrollItem.setCardnumber(StringUtils.isEmpty(row.getCell(2))?"":replaceSpace(row.getCell(2).toString()));
                personalPayrollItem.setItemname(StringUtils.isEmpty(row.getCell(4))?"":row.getCell(4).toString());//不去空格
                
                
                //默认值
                personalPayrollItem.setAlreadydeclarewage(new BigDecimal(0));
                personalPayrollItem.setCompanyamount(new BigDecimal(0));
                //构参,收入期起
                String startDate = null;
                logger.info("收入所属期起{}",row.getCell(5));
                if (StringUtils.isNotEmpty(row.getCell(5))){
                    if (CellType.NUMERIC.equals(row.getCell(5).getCellTypeEnum())){
                        Date value = row.getCell(5).getDateCellValue();
                        if (null != value){
                            startDate = DateUtils.longToDateString(value.getTime(), DateUtils.ZH_PATTERN_DAY);
                        }
                    }
                }
                
                if (StringUtils.isNotEmpty(startDate)){
                    if ( !startDate.matches(regex)){
                        message.add("收入所属期起格式错误");
                        code = 1;
                    }else {
                        personalPayrollItem.setBegindate(startDate);  
                    }
                }
                //收入期止
                String endDate = null;
                logger.info("收入所属期止{}",row.getCell(6));
                if (StringUtils.isNotEmpty(row.getCell(6))){
                    if (CellType.NUMERIC.equals(row.getCell(6).getCellTypeEnum())){
                        Date value = row.getCell(6).getDateCellValue();
                        if (null != value){
                            endDate = DateUtils.longToDateString(value.getTime(), DateUtils.ZH_PATTERN_DAY);
                        }
                    }
                }
                if (StringUtils.isNotEmpty(endDate)){
                    if ( !endDate.matches(regex)){
                        message.add("收入所属期止格式错误");
                        code = 1;
                    }else {
                        personalPayrollItem.setEnddate(endDate);
                    }
                }
                
                if (setValue(row.getCell(7).toString()) == 1) {
                    message.add("收入额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setWage(setFinalValue(row.getCell(7).toString()));
                }
                
                if (setValue(row.getCell(8).toString()) == 1) {
                    message.add("免税所得格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setDutyfreeamount(setFinalValue(row.getCell(8).toString()));
                }

                if (setValue(row.getCell(9).toString()) == 1) {
                    message.add("基本养老保险费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setPension(setFinalValue(row.getCell(9).toString()));
                }

                if (setValue(row.getCell(10).toString()) == 1) {
                    message.add("基本医疗保险费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setHealthinsurance(setFinalValue(row.getCell(10).toString()));
                }

                if (setValue(row.getCell(11).toString()) == 1) {
                    message.add("失业保险费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setUnemploymentinsurance(setFinalValue(row.getCell(11).toString()));
                }

                if (setValue(row.getCell(12).toString()) == 1) {
                    message.add("住房公积金格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setHousingfund(setFinalValue(row.getCell(12).toString()));
                }

                if (setValue(row.getCell(13).toString()) == 1) {
                    message.add("财产原值格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setOriginalproperty(setFinalValue(row.getCell(13).toString()));
                }

                if (setValue(row.getCell(14).toString()) == 1) {
                    message.add("允许扣除的税费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setAllowdeduction(setFinalValue(row.getCell(14).toString()));
                }

                if (setValue(row.getCell(18).toString()) == 1) {
                    message.add("其他扣除格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setOther(setFinalValue(row.getCell(18).toString()));
                }

                if (setValue(row.getCell(19).toString()) == 1) {
                    message.add("合计格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setTotal(setFinalValue(row.getCell(19).toString()));
                }

                if (setValue(row.getCell(20).toString()) == 1) {
                    message.add("减除费用格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setDeductionamount(setFinalValue(row.getCell(20).toString()));
                }

                if (setValue(row.getCell(21).toString()) == 1) {
                    message.add("准予扣除的捐赠额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setDeductiondonate(setFinalValue(row.getCell(21).toString()));
                }

                if (setValue(row.getCell(22).toString()) == 1) {
                    message.add("应纳税所得额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setTaxincome(setFinalValue(row.getCell(22).toString()));
                }

                if (setValue(row.getCell(23).toString()) == 1) {
                    message.add("税率格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setTaxrate(setFinalValue(row.getCell(23).toString()));
                }

                if (setValue(row.getCell(24).toString()) == 1) {
                    message.add("速算扣除数格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setSpeeddeduction(setFinalValue(row.getCell(24).toString()));
                }

                if (setValue(row.getCell(25).toString()) == 1) {
                    message.add("应纳税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setShouldpaytax(setFinalValue(row.getCell(25).toString()));
                }

                if (setValue(row.getCell(27).toString()) == 1) {
                    message.add("减免税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setRelieftax(setFinalValue(row.getCell(27).toString()));
                }

                if (setValue(row.getCell(28).toString()) == 1) {
                    message.add("应扣缴税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setShouldcosttax(setFinalValue(row.getCell(28).toString()));
                }

                if (setValue(row.getCell(29).toString()) == 1) {
                    message.add("已扣缴税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem
                        .setAlreadycosttax(setFinalValue(row.getCell(29).toString()));
                }

                if (setValue(row.getCell(30).toString()) == 1) {
                    message.add("应补（退）税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setFinallytax(setFinalValue(row.getCell(30).toString()));
                }

                personalPayrollItem.setRemarks(row.getCell(31).toString());
                if (setValue(row.getCell(17).toString()) == 1) {
                    message.add("投资抵扣格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setDeduction(setFinalValue(row.getCell(17).toString()));
                }

                if (setValue(row.getCell(15).toString()) == 1) {
                    message.add("年金格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setAnnuity(setFinalValue(row.getCell(15).toString()));
                }

                if (setValue(row.getCell(16).toString()) == 1) {
                    message.add("商业健康险格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setInsurance(setFinalValue(row.getCell(16).toString()));
                }

                if (code == 1) {
                    BeanUtils.copyProperties(personalPayrollItem, bizTaxPersonalPayrollItem);
                    bizTaxPersonalPayrollItem.setCode(1);
                    bizTaxPersonalPayrollItem.setMessage(message);
                    bizTaxPersonalPayrollItems.add(bizTaxPersonalPayrollItem);
                    continue;
                }

                personalPayrollItem
                    .setBizTaxInstanceCategoryPersonalPayrollId(importSheetDTO.getCategoryId());
                taxInstanceCategoryPersonalPayrollItemFactory
                    .initialize(personalPayrollItem, importSheetDTO.getMdInstClientId(),importSheetDTO.getMdCompanyId(),
                        importSheetDTO.getMdAccountCycleId(),
                        importSheetDTO.getTaxareaId());
                bizTaxInstanceCategoryPersonalPayrollItemStateDTO = taxInstanceCategoryPersonalPayrollItemFactory
                    .vaild(personalPayrollItem, importSheetDTO.getMdCompanyId(),
                        importSheetDTO.getMdInstClientId());
                int flag = bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getCode();
                if (flag == 0) {
                    BeanUtils.copyProperties(personalPayrollItem, bizTaxPersonalPayrollItem);
                    bizTaxPersonalPayrollItem.setCode(0);
                    bizTaxPersonalPayrollItems.add(bizTaxPersonalPayrollItem);
                } else {
                    BeanUtils.copyProperties(personalPayrollItem, bizTaxPersonalPayrollItem);
                    bizTaxPersonalPayrollItem.setCode(flag);
                    bizTaxPersonalPayrollItem
                        .setMessage(bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getMessage());
                    bizTaxPersonalPayrollItems.add(bizTaxPersonalPayrollItem);
                }


            }
        }
        return bizTaxPersonalPayrollItems;
    }
    
    //设置cellType
    private void setFORMULAValue(Row row) {
        for (int i = 0; i < 32; i++) {
            if (StringUtils.isNotEmpty(row.getCell(i))) {
                if (CellType.NUMERIC.equals(row.getCell(i).getCellTypeEnum())){//日期
                    continue;
                }else if (CellType.FORMULA.equals(row.getCell(i).getCellTypeEnum())){//公式
                    try {
                        double iCellValue = row.getCell(i).getNumericCellValue();
                        row.getCell(i).setCellType(CellType.NUMERIC);
                        row.getCell(i).setCellValue(iCellValue);
                    } catch (Exception e) {
                        continue;
                    }
                }else {
                    row.getCell(i).setCellType(CellType.STRING);
                }
            }else {
                row.getCell(i).setCellValue("");
            }
        }
    }

    /**
     * 解析csv
     *
     * @param inputStream 流
     */
    private List<BizTaxPersonalPayrollItem> readCSVPersonal(InputStream inputStream,
        BizTaxPersonalImportSheetDTO importSheetDTO, String sn) {

        CSVReader reader = null;
        List<BizTaxPersonalPayrollItem> bizTaxPersonalPayrollItems = new ArrayList<>();
        List<String> message;
        BizTaxInstanceCategoryPersonalPayrollItemStateDTO bizTaxInstanceCategoryPersonalPayrollItemStateDTO;
        try {
            reader = new CSVReader(new InputStreamReader(inputStream, "gbk"));
            String[] strArr;
            int i = 1;
            while ((strArr = reader.readNext()) != null) {
                if (i == 1) {
                    i++;
                    continue;
                }
                if (i == 2) {
                    if (strArr[0].contains("纳税人名称") && strArr[1].contains("身份证照类型")) {
                        i++;
                        continue;
                    } else {
                        throw BizTaxException
                            .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION,
                                "请按照下载的模板提交");
                    }
                }

                int code = 0;
                BizTaxPersonalPayrollItem bizTaxPersonalPayrollItem = new BizTaxPersonalPayrollItem();
                BizTaxInstanceCategoryPersonalPayrollItem personalPayrollItem = new BizTaxInstanceCategoryPersonalPayrollItem();

                if (StringUtils.isEmpty(strArr[0]) && StringUtils.isEmpty(strArr[1]) && StringUtils
                    .isEmpty(strArr[2]) && StringUtils.isEmpty(strArr[3]) && StringUtils
                    .isEmpty(strArr[4]) && StringUtils.isEmpty(strArr[7])) {
                    continue;
                }

                personalPayrollItem.setSort(i);
                personalPayrollItem.setName(StringUtils.isEmpty(strArr[0])?"":replaceSpace(strArr[0]));
                personalPayrollItem.setCountryname(StringUtils.isEmpty(strArr[3])?"":replaceSpace(strArr[3]));
                personalPayrollItem.setCardname(StringUtils.isEmpty(strArr[1])?"":replaceSpace(strArr[1]));
                personalPayrollItem.setCardnumber(StringUtils.isEmpty(strArr[2])?"":replaceSpace(strArr[2]));
                personalPayrollItem.setItemname(StringUtils.isEmpty(strArr[3])?"":strArr[4]);//不去空格
                //默认值
                personalPayrollItem.setAlreadydeclarewage(new BigDecimal(0));
                personalPayrollItem.setCompanyamount(new BigDecimal(0));

                message = new ArrayList<>();
               
                if (StringUtils.isNotEmpty(strArr[5])){
                    if (strArr[5].matches(regex)){
                        personalPayrollItem.setBegindate(strArr[5]);
                    }else {
                        message.add("收入所属期起格式错误");
                        code = 1;
                    }
                }else {
                    personalPayrollItem.setBegindate("");
                }
                if (StringUtils.isNotEmpty(strArr[6])){
                    if (strArr[6].matches(regex)){
                        personalPayrollItem.setEnddate(strArr[6]);
                    }else {
                        message.add("收入所属期止格式错误");
                        code = 1;
                    }
                }else {
                    personalPayrollItem.setEnddate("");
                }

                if (setValue(strArr[7]) == 1) {
                    message.add("收入额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setWage(setFinalValue(strArr[7]));
                }

                if (setValue(strArr[8]) == 1) {
                    message.add("免税所得格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setDutyfreeamount(setFinalValue(strArr[8]));
                }

                if (setValue(strArr[9]) == 1) {
                    message.add("基本养老保险费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setPension(setFinalValue(strArr[9]));
                }

                if (setValue(strArr[10]) == 1) {
                    message.add("基本医疗保险费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setHealthinsurance(setFinalValue(strArr[10]));
                }

                if (setValue(strArr[11]) == 1) {
                    message.add("失业保险费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setUnemploymentinsurance(setFinalValue(strArr[11]));
                }

                if (setValue(strArr[12]) == 1) {
                    message.add("住房公积金格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setHousingfund(setFinalValue(strArr[12]));
                }

                if (setValue(strArr[13]) == 1) {
                    message.add("财产原值格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setOriginalproperty(setFinalValue(strArr[13]));
                }

                if (setValue(strArr[14]) == 1) {
                    message.add("允许扣除的税费格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setAllowdeduction(setFinalValue(strArr[14]));
                }

                if (setValue(strArr[18]) == 1) {
                    message.add("其他扣除格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setOther(setFinalValue(strArr[18]));
                }

                if (setValue(strArr[19]) == 1) {
                    message.add("合计格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setTotal(setFinalValue(strArr[19]));
                }

                if (setValue(strArr[20]) == 1) {
                    message.add("减除费用格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setDeductionamount(setFinalValue(strArr[20]));
                }

                if (setValue(strArr[21]) == 1) {
                    message.add("准予扣除的捐赠额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setDeductiondonate(setFinalValue(strArr[21]));
                }

                if (setValue(strArr[22]) == 1) {
                    message.add("应纳税所得额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setTaxincome(setFinalValue(strArr[22]));
                }

                if (setValue(strArr[23]) == 1) {
                    message.add("税率格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setTaxrate(setFinalValue(strArr[23]));
                }

                if (setValue(strArr[24]) == 1) {
                    message.add("速算扣除数格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setSpeeddeduction(setFinalValue(strArr[24]));
                }

                if (setValue(strArr[25]) == 1) {
                    message.add("应纳税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setShouldpaytax(setFinalValue(strArr[25]));
                }

                if (setValue(strArr[27]) == 1) {
                    message.add("减免税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setRelieftax(setFinalValue(strArr[27]));
                }

                if (setValue(strArr[28]) == 1) {
                    message.add("应扣缴税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setShouldcosttax(setFinalValue(strArr[28]));
                }

                if (setValue(strArr[29]) == 1) {
                    message.add("已扣缴税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setAlreadycosttax(setFinalValue(strArr[29]));
                }

                if (setValue(strArr[30]) == 1) {
                    message.add("应补（退）税额格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setFinallytax(setFinalValue(strArr[30]));
                }

                personalPayrollItem.setRemarks(strArr[31]);
                if (setValue(strArr[17]) == 1) {
                    message.add("投资抵扣格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setDeduction(setFinalValue(strArr[17]));
                }

                if (setValue(strArr[15]) == 1) {
                    message.add("年金格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setAnnuity(setFinalValue(strArr[15]));
                }

                if (setValue(strArr[16]) == 1) {
                    message.add("商业健康险格式错误");
                    code = 1;
                } else {
                    personalPayrollItem.setInsurance(setFinalValue(strArr[16]));
                }

                if (code == 1) {
                    bizTaxPersonalPayrollItem.setCode(1);
                    bizTaxPersonalPayrollItem.setMessage(message);
                    bizTaxPersonalPayrollItems.add(bizTaxPersonalPayrollItem);
                    i++;
                    continue;
                }
                bizTaxPersonalPayrollItem
                    .setBizTaxInstanceCategoryPersonalPayrollId(importSheetDTO.getCategoryId());
                //校验前去空格
                taxInstanceCategoryPersonalPayrollItemFactory
                    .initialize(personalPayrollItem, importSheetDTO.getMdCompanyId(),
                        importSheetDTO.getMdAccountCycleId(), importSheetDTO.getMdInstClientId(),
                        importSheetDTO.getTaxareaId());
                bizTaxInstanceCategoryPersonalPayrollItemStateDTO = taxInstanceCategoryPersonalPayrollItemFactory
                    .vaild(personalPayrollItem, importSheetDTO.getMdCompanyId(),
                        importSheetDTO.getMdInstClientId());
                int flag = bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getCode();
                if (flag == 0) {
                    BeanUtils.copyProperties(personalPayrollItem, bizTaxPersonalPayrollItem);
                    bizTaxPersonalPayrollItem.setCode(0);
                    bizTaxPersonalPayrollItems.add(bizTaxPersonalPayrollItem);
                } else {
                    BeanUtils.copyProperties(personalPayrollItem, bizTaxPersonalPayrollItem);
                    bizTaxPersonalPayrollItem.setCode(flag);
                    bizTaxPersonalPayrollItem
                        .setMessage(bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getMessage());
                    bizTaxPersonalPayrollItems.add(bizTaxPersonalPayrollItem);
                }

                i++;
            }
        } catch (Exception e) {
            logger.debug("POI解析Excel输入流异常", e);
            throw BizTaxException
                .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION, "解析csv异常,");
        } finally {
            logger.debug("关闭文件输入流");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bizTaxPersonalPayrollItems;
    }

    private int setValue(String strArr) {
        //不填数据默认为0
        if(StringUtils.isEmpty(strArr)){
            return 0;
        }
        try {
            new BigDecimal(strArr);
        } catch (Exception e) {
            return 1;
        }
        return 0;
    }
    //不填数据默认为0
    private BigDecimal setFinalValue(String initValue){
      return  StringUtils.isEmpty(initValue) ? new BigDecimal("0.00") : new BigDecimal(initValue);
    }
    //去空格
    private String replaceSpace(String srcStr){
        return srcStr.replaceAll("\\s*", "");
    }

    private List<BizTaxPersonalPayrollItem> parseExcelPersonal(String fileType,
        InputStream inputStream, BizTaxPersonalImportSheetDTO importSheetDTO, String sn) {
        Workbook wb = FileParse.readExcel(fileType, inputStream);
        int sheetNumber = wb.getNumberOfSheets();
        logger.debug("解析Excel文件，共找到{}张Sheet", sheetNumber);
        List<BizTaxPersonalPayrollItem> auditSheet = null;
        for (int i = 0; i < 1; i++) {
            //构建数据行
            auditSheet = buildPersonalSheet(wb.getSheetAt(i), importSheetDTO, sn);
        }
        //auditSheet = buildPersonalSheet(wb.getSheetAt(0),importSheetDTO,sn);
        return auditSheet;
    }


    @Override
    public List<PersonalHistoryPayrollDTO> getHistoryList(Long instClientId, long mdCompanyId, long areaId,
        long[] mdAccountCycleIdList) {
        //获取税种id
        BizTaxMdCategory bizTaxMdCategory = Optional
            .ofNullable(bizTaxMdCategoryService.findBySn(TaxSn.m_personal_payroll)).orElseThrow(
                () -> BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound));

        //获取某机构客户的申报历史
        List<PersonalHistoryPayrollDTO> historyPayrollDtoList = new ArrayList<PersonalHistoryPayrollDTO>();
        for (long cycleId : mdAccountCycleIdList) {
            BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findById(cycleId);
            if (bizMdAccountCycle == null) {
                continue;
            }

            BizTaxInstance cycleBizTaxInstance = bizTaxInstanceService
                .currentTaxOfficeInstClientInstance(instClientId, mdCompanyId, bizMdAccountCycle.getId(),
                    TaxOffice.ds, areaId);
            if (cycleBizTaxInstance == null) {
                continue;
            }

            BizTaxInstanceCategory cycleBizTaxInstanceCategory = bizTaxInstanceCategoryService
                .findByInstanceAndTaxMdCategoryId(cycleBizTaxInstance.getId(),
                    bizTaxMdCategory.getId());
            if (cycleBizTaxInstanceCategory == null) {
                continue;
            }

            BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = bizTaxInstanceCategoryPersonalPayrollService
                .findByInstanceCategoryId(cycleBizTaxInstanceCategory.getId());
            if (bizTaxInstanceCategoryPersonalPayrollService == null) {
                continue;
            }

            PersonalHistoryPayrollDTO personalHistoryPayrollDto = new PersonalHistoryPayrollDTO() {{
                setId(bizTaxInstanceCategoryPersonalPayroll.getId());
                setCycle(bizMdAccountCycle.getSn());
                setCycleId(bizMdAccountCycle.getId());
                setPopleNum(bizTaxInstanceCategoryPersonalPayroll.getPopleNum());
                setTaxAmount(bizTaxInstanceCategoryPersonalPayroll.getTaxAmount());
            }};
            historyPayrollDtoList.add(personalHistoryPayrollDto);
        }
        return historyPayrollDtoList;
    }

    @Override
    public BizTaxPersonalImportDTO getHistoryItems(Long payrollId) {
        BizTaxInstanceCategoryPersonalPayroll payroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService.findById(payrollId)).orElseThrow(()->BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR,"个税税种实例不存在"));

        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(payroll.getBizTaxInstanceCategory()).orElseThrow(()->BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR,"税种实例不存在"));

        BizTaxInstance bizTaxInstance = Optional.ofNullable(bizTaxInstanceCategory.getBizTaxInstance()).orElseThrow(()->BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR,"税种不存在"));

        List<BizTaxInstanceCategoryPersonalPayrollItem> itemList = bizTaxInstanceCategoryPersonalPayrollItemService
            .findByCategoryPersonalPayrollId(payrollId);

        List<BizTaxPersonalPayrollItem> valiedItemList = new ArrayList<BizTaxPersonalPayrollItem>();
        //校验
        if (CollectionUtils.isNotEmpty(itemList)){
            itemList.stream().forEach(item->{
                BizTaxInstanceCategoryPersonalPayrollItemStateDTO validateDto = taxInstanceCategoryPersonalPayrollItemFactory.vaild(item,bizTaxInstance.getMdCompanyId(),bizTaxInstance.getMdInstClientId());
                BizTaxPersonalPayrollItem valiedItem = new BizTaxPersonalPayrollItem();
                BeanUtils.copyProperties(item,valiedItem);
                valiedItem.setCode(validateDto.getCode());
                valiedItem.setMessage(new ArrayList(){{
                    add(validateDto.getMessage());
                }});

                valiedItemList.add(valiedItem);
            });
        }

        //检查重复
        return separateItem(valiedItemList);
    }

    private BizTaxPersonalImportDTO separateItem(List<BizTaxPersonalPayrollItem> itemList){
        BizTaxPersonalImportDTO backDto = new BizTaxPersonalImportDTO();
        Map<String,BizTaxPersonalPayrollItem> itemMap = new HashMap<String,BizTaxPersonalPayrollItem>();
        if (CollectionUtils.isNotEmpty(itemList)){
            itemList.stream().forEach(item->{
                //去除空格
                item.setName(item.getName()==null?"":replaceSpace(item.getName()));
                item.setCountryid(item.getCountryid()==null?"":replaceSpace(item.getCountryid()));
                item.setCardtype(item.getCardtype()==null?"":replaceSpace(item.getCardtype()));
                item.setCardname(item.getCardname()==null?"":replaceSpace(item.getCardname()));
                item.setCardnumber(item.getCardnumber()==null?"":replaceSpace(item.getCardnumber()));
                item.setCountryname(item.getCountryname()==null?"":replaceSpace(item.getCountryname()));
                item.setItemcode(item.getItemcode()==null?"":replaceSpace(item.getItemcode()));
                item.setItemname(item.getItemname()==null?"":replaceSpace(item.getItemname()));
                item.setTaxburdentype(item.getTaxburdentype()==null?"":replaceSpace(item.getTaxburdentype()));
                item.setDetailcode(item.getDetailcode()==null?"":replaceSpace(item.getDetailcode()));
                item.setDetailname(item.getDetailname()==null?"":replaceSpace(item.getDetailname()));
                item.setBegindate(item.getBegindate()==null?"":replaceSpace(item.getBegindate()));
                item.setEnddate(item.getEnddate()==null?"":replaceSpace(item.getEnddate()));

                String key = item.getCardnumber() + item.getItemcode();
                if (itemMap.get(key) != null){
                    backDto.getRepeatSheet().add(item);
                    backDto.getRepeatSheet().add(itemMap.get(key));
                    backDto.getSingleSheet().removeIf(singleItem->singleItem.getCardnumber()+singleItem.getItemcode() == key);
                }else{
                    backDto.getSingleSheet().add(item);
                }
                itemMap.put(key,item);
            });
        }
        //去重
        List<BizTaxPersonalPayrollItem> repeatSheet = backDto.getRepeatSheet();
        List<BizTaxPersonalPayrollItem> singleSheet = backDto.getSingleSheet();
        List<String> keys = new ArrayList<String>(){{
            repeatSheet.forEach(v -> add(v.getCardnumber()+v.getItemcode())
            );
        }};
        List<BizTaxPersonalPayrollItem> repeatItems = new ArrayList<BizTaxPersonalPayrollItem>(){{
            singleSheet.forEach(v -> {
                String key = v.getCardnumber()+v.getItemcode();
                if (keys.contains(key)){
                    add(v);
                }
            });
        }};
        repeatItems.forEach(v ->{
            if (singleSheet.contains(v)){
                singleSheet.remove(v);
            }
        });
        backDto.setRepeatSheet(repeatSheet);
        backDto.setSingleSheet(singleSheet);
        return backDto;
    }

    @Override
    public boolean updateSourceType(long instanceCategoryPersonalPayrollId,
        BizTaxInstanceCategoryPersonalPayroll.SourceType sourceType, long userId) {
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService
            .findById(instanceCategoryPersonalPayrollId))
            .orElseThrow(() -> BizTaxException
                .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有公司对象"));
        boolean back = bizTaxInstanceCategoryPersonalPayrollService
            .updateSourceType(instanceCategoryPersonalPayrollId, sourceType);
        if (!back){
            return back;
        }
        //若按上月调用审核
        if (BizTaxInstanceCategoryPersonalPayroll.SourceType.last.equals(sourceType)){
            BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayrollNew = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService
                    .findById(bizTaxInstanceCategoryPersonalPayroll.getId()))
                    .orElseThrow(() -> BizTaxException
                            .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有公司对象"));
            auditOrNot(userId,"confirmed",bizTaxInstanceCategoryPersonalPayrollNew);
        }
        return back;
    }


    @Override
    public void audit(long taxInstanceId, long userId, String action) {
        //查询税种实例
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(
                bizTaxInstanceCategoryPersonalPayrollService.findById(taxInstanceId)).orElseThrow(() -> 
                BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "请求参数错误,税种实例不存在"));
        //可用状态
        if (bizTaxInstanceCategoryPersonalPayroll.getDisabled() != 0) {
            throw BizTaxException
                    .build(BizTaxException.Codes.BizTaxException, "操作失败,税种实例不可用");
        }
        //根据申报类型和操作进行不同的操作
        if (BizTaxInstanceCategoryPersonalPayroll.SourceType.hand.equals(
                bizTaxInstanceCategoryPersonalPayroll.getSourceType())){
            //按实际
            //checkAccountCycle(bizTaxInstanceCategoryPersonalPayroll.getIncomeAccountCycleId());
            //调用审核
            auditOrNot(userId, action, bizTaxInstanceCategoryPersonalPayroll);
        }else if (BizTaxInstanceCategoryPersonalPayroll.SourceType.last.equals(
                bizTaxInstanceCategoryPersonalPayroll.getSourceType())){
            //按上月
            //checkAccountCycle(bizTaxInstanceCategoryPersonalPayroll.getIncomeAccountCycleId());
            //审核
            auditOrNot(userId, action, bizTaxInstanceCategoryPersonalPayroll);
        }else {
            throw BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "当前申报类型是"+bizTaxInstanceCategoryPersonalPayroll.getSourceType()+",不能进行审核操作,请修改申报类型后操作！");
        }
    }

    @Override
    public void cancleAudit(long taxInstanceCategoryPayrollId, long userId,String action) {
        //查询税种实例
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional
                .ofNullable(
                        bizTaxInstanceCategoryPersonalPayrollService.findById(taxInstanceCategoryPayrollId)).orElseThrow(
                        () -> BizTaxException
                                .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "请求参数错误,税种实例不存在"));
        //可用状态
        if (bizTaxInstanceCategoryPersonalPayroll.getDisabled() != 0) {
            throw BizTaxException
                    .build(BizTaxException.Codes.BizTaxException, "操作失败,税种实例不可用");
        }
        //撤销审核
        auditOrNot(userId,action,bizTaxInstanceCategoryPersonalPayroll);
    }

    private void checkAccountCycle(Long accountCycleIdExist) {
        BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(
                bizMdAccountCycleService.findById(accountCycleIdExist))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "申报所属会计区间不存在"));

        if ( !bizMdAccountCycle.isEnable()){
            throw  BizTaxException
                    .build(BizTaxException.Codes.BizTaxException, "无效的收入所属会计区间！");
        }

        long now = DateUtils.stringDateToTimeStampSecs(
                DateUtils.getCurrMonthFirstDay(Instant.now().toEpochMilli(), DateUtils.ZH_PATTERN_DAY), DateUtils.ZH_PATTERN_DAY);
        if (bizMdAccountCycle.getEndDate() > now){
            throw  BizTaxException
                    .build(BizTaxException.Codes.BizTaxException, "会计区间有误！");
        }
    }

    private void auditOrNot(long userId, String action, BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll) {
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryPersonalPayroll.getBizTaxInstanceCategory();
        if (null == bizTaxInstanceCategory ){
            throw  BizTaxException
                    .build(BizTaxException.Codes.BizTaxException, "该公司申报实例不存在！");
        }
        if ("unconfirmed".equals(action)) {
            //调用统一撤销审核
            if ( !bizTaxInstanceCategory.isAudit()) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "该公司未审核,无需取消审核");
            }
            taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(),userId);
        } else if ("confirmed".equals(action)) {
            //调用统一审核
            if (bizTaxInstanceCategory.isAudit()){
               throw  BizTaxException
                        .build(BizTaxException.Codes.BizTaxException, "该公司已审核,无需再次审核");
            }
            //按实际校验人员清单
            if (BizTaxInstanceCategoryPersonalPayroll.SourceType.hand.equals(bizTaxInstanceCategoryPersonalPayroll.getSourceType())){
                BizTaxInstance bizTaxInstance = bizTaxInstanceService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategoryPersonalPayroll.getId());
                if (null != bizTaxInstance) {
                    long mdCompanyId = bizTaxInstance.getMdCompanyId();
                    long mdClientId = bizTaxInstance.getMdInstClientId();
                    //获取人员清单
                    List<BizTaxInstanceCategoryPersonalPayrollItem> payrollList = bizTaxInstanceCategoryPersonalPayrollItemService.findByBizTaxInstanceCategoryPersonalPayrollIdAndUseType(bizTaxInstanceCategoryPersonalPayroll.getId(), BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare);
                    //校验
                    if (payrollList.size() > 0) {
                        payrollList.forEach(v -> {
                            BizTaxInstanceCategoryPersonalPayrollItemStateDTO payrollItemStateDTO =
                                    taxInstanceCategoryPersonalPayrollItemFactory.vaild(v, mdCompanyId, mdClientId);
                            int code = payrollItemStateDTO.getCode();
                            if (code == 1) {
                                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "人員清單数据校验未通过,错误类型:" + payrollItemStateDTO.getMessage());
                            } else if (code == 2) {
                                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "人員清單数据校验未通过,数据重复:");
                            }
                        });
                    } else {
                        BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "按实际申报,校验人员清单,未找到人员清单信息");
                    }
                } else {
                    BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到税种实例");
                }
            }
            //调用数据统计
            bizTaxInstanceCategoryPersonalPayrollService.updatePopleNumAndTaxAmountById(bizTaxInstanceCategoryPersonalPayroll.getId());
            //保存yun9应纳税额
            BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayrollNew = Optional.ofNullable(
                    bizTaxInstanceCategoryPersonalPayrollService.findById(bizTaxInstanceCategoryPersonalPayroll.getId())).orElseThrow(() ->
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "请求参数错误,税种实例不存在"));
            BigDecimal taxAmount = bizTaxInstanceCategoryPersonalPayrollNew.getTaxAmount();
            bizTaxInstanceCategory.setTaxPayAmount(taxAmount);
            bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);
            //审核
            taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
                @Override
                public void success() {
                    //不需生成报表
                   
                }
                @Override
                public void exception(BizException ex) {
                    throw ex;
                }
            });
        }
    }

    @Override
    public BizTaxInstanceCategoryPersonalPayrollItemStateDTO addPersonalPayrollItem(List<BizTaxInstanceCategoryPersonalPayrollItem> bizTaxInstanceCategoryPersonalPayrollItemList, long categoryId, boolean isDelete) {
        BizTaxInstanceCategoryPersonalPayrollItemStateDTO result = new BizTaxInstanceCategoryPersonalPayrollItemStateDTO();
        if (CollectionUtils.isNotEmpty(bizTaxInstanceCategoryPersonalPayrollItemList)) {
            for (int i = 0; i < bizTaxInstanceCategoryPersonalPayrollItemList.size(); i++) {
                BizTaxInstanceCategoryPersonalPayrollItem item = bizTaxInstanceCategoryPersonalPayrollItemList.get(i);

                item = checkObject(item, DataOperationType.CREATE.toString());

                BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryPersonalPayrollService.findById(item.getBizTaxInstanceCategoryPersonalPayrollId()).getBizTaxInstanceCategory();

                //查询客户ID和机构ID

                BizTaxInstance bizTaxInstance =bizTaxInstanceCategoryService.findById(bizTaxInstanceCategory.getId()).getBizTaxInstance(); // bizTaxInstanceService.findByBizTaxInstanceCategoryId(item.getBizTaxInstanceCategoryPersonalPayrollId());
                if (null == bizTaxInstance) {
                    throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "没有找到客户信息和机构信息!");
                }

                item = taxInstanceCategoryPersonalPayrollItemFactory.initialize(item, bizTaxInstance.getMdInstClientId(), bizTaxInstance.getMdCompanyId(), bizTaxInstance.getMdAccountCycleId(), bizTaxInstance.getMdAreaId());


                //数据校验
                BizTaxInstanceCategoryPersonalPayrollItemStateDTO bizTaxInstanceCategoryPersonalPayrollItemStateDTO = taxInstanceCategoryPersonalPayrollItemFactory.vaild(item, bizTaxInstance.getMdCompanyId(), bizTaxInstance.getMdInstClientId());
                int code = bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getCode();

                //校验不通过
                if (code == 1) {
                    result.setCode(500);
                    result.setMessage(new ArrayList() {{
                        add("个人所得税个体薪金数据校验错误:" + bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getMessage());
                    }});
                    return result;
                } else if (code == 2) {//重复记录
                    if (!isDelete) {//若不需要删除原来的就报错
                        result.setCode(500);
                        result.setId(bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getId());
                        result.setMessage(new ArrayList() {{
                            add("该用户已经存在");
                        }});

                        return result;
                    }
                }
            }
        }


        //调用保存服务
        bizTaxInstanceCategoryPersonalPayrollItemService.create(categoryId, isDelete, bizTaxInstanceCategoryPersonalPayrollItemList);

        //bizTaxInstanceCategoryPersonalPayrollService.updatePopleNumAndTaxAmountById(bizTaxInstanceCategoryPersonalPayrollItemList.get(0).getBizTaxInstanceCategoryPersonalPayrollId());

        result.setCode(200);
        result.setMessage(new ArrayList() {{
            add("保存成功");
        }});

        return result;
    }


    @Override
    public BizTaxInstanceCategoryPersonalPayrollItemStateDTO updatePersonalPayrollItem(
        BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem) {

        BizTaxInstanceCategoryPersonalPayrollItemStateDTO result = new BizTaxInstanceCategoryPersonalPayrollItemStateDTO();

        List<String> content = new ArrayList<>();

        bizTaxInstanceCategoryPersonalPayrollItem = checkObject(bizTaxInstanceCategoryPersonalPayrollItem, DataOperationType.UPDATE.toString());

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryPersonalPayrollService.findById(bizTaxInstanceCategoryPersonalPayrollItem.getBizTaxInstanceCategoryPersonalPayrollId()).getBizTaxInstanceCategory();

        //查询客户ID和机构ID
        BizTaxInstance bizTaxInstance =bizTaxInstanceCategoryService.findById(bizTaxInstanceCategory.getId()).getBizTaxInstance(); // bizTaxInstanceService.findByBizTaxInstanceCategoryId(item.getBizTaxInstanceCategoryPersonalPayrollId());

        if (null == bizTaxInstance) {

            throw BizTaxException
                .throwException(BizTaxException.Codes.BizTaxException, "没有找到客户信息和机构信息!");

        }

        bizTaxInstanceCategoryPersonalPayrollItem = taxInstanceCategoryPersonalPayrollItemFactory.initialize(bizTaxInstanceCategoryPersonalPayrollItem, bizTaxInstance.getMdInstClientId(), bizTaxInstance.getMdCompanyId(), bizTaxInstance.getMdAccountCycleId(), bizTaxInstance.getMdAreaId());

        //数据校验
        BizTaxInstanceCategoryPersonalPayrollItemStateDTO bizTaxInstanceCategoryPersonalPayrollItemStateDTO = taxInstanceCategoryPersonalPayrollItemFactory
            .vaild(bizTaxInstanceCategoryPersonalPayrollItem, bizTaxInstance.getMdCompanyId(),
                bizTaxInstance.getMdInstClientId());

        int code = bizTaxInstanceCategoryPersonalPayrollItemStateDTO.getCode();

        try {

            switch (code) {

                case 0:

                    bizTaxInstanceCategoryPersonalPayrollItemService.update(bizTaxInstanceCategoryPersonalPayrollItem);

                    // bizTaxInstanceCategoryPersonalPayrollService.updatePopleNumAndTaxAmountById(bizTaxInstanceCategoryPersonalPayrollItem.getBizTaxInstanceCategoryPersonalPayrollId());

                    content.add("保存成功");

                    result.setCode(200);
                    result.setMessage(content);

                    break;

                case 1:

                    content.add(
                        "个人所得税个体薪金数据校验错误:" + bizTaxInstanceCategoryPersonalPayrollItemStateDTO
                            .getMessage());

                    result.setCode(500);
                    result.setMessage(content);
                    break;

            }


        } catch (Exception e) {

            e.printStackTrace();

            content.add("保存失败:" + e.getMessage());

            result.setCode(500);
            result.setMessage(content);

        }

        return result;

    }

    @Override
    public Object deletePersonalPayrollItem(List<Long> ids) {

        BizTaxInstanceCategoryPersonalPayrollItemStateDTO result = new BizTaxInstanceCategoryPersonalPayrollItemStateDTO();

        List<String> content = new ArrayList<>();

        try {


            long bizTaxInstanceCategoryPersonalPayrollId = bizTaxInstanceCategoryPersonalPayrollItemService.batchDelete(ids);

            //bizTaxInstanceCategoryPersonalPayrollService.updatePopleNumAndTaxAmountById(bizTaxInstanceCategoryPersonalPayrollId);

            content.add("删除成功");

            result.setCode(200);
            result.setMessage(content);


        } catch (Exception e) {

            e.printStackTrace();

            content.add("删除失败:" + e.getMessage());

            result.setCode(500);
            result.setMessage(content);

        }

        return result;


    }

    @Override
    public Object getPersonalPayrollItem(int page, int limit, Map<String, Object> params) {

        Map<String, Object> result = new HashMap();

        List<String> content = new ArrayList<>();

        try {

            Pagination<BizTaxInstanceCategoryPersonalPayrollItem> valueList = bizTaxInstanceCategoryPersonalPayrollItemService
                .pageByCondition(page, limit, params);

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
    public Object confirmPersonalInfo(long categoryPersonalPayrollId) {

        Map<String, Object> result = new HashMap();

        List<String> content = new ArrayList<>();

        try {

            HashMap value = bizTaxInstanceCategoryPersonalPayrollService
                .confirmPersonList(categoryPersonalPayrollId);

            content.add("查询成功");

            result.put("code", 200);
            result.put("message", content);
            result.put("content", value);


        } catch (Exception e) {

            e.printStackTrace();

            content.add("查询失败:" + e.getMessage());

            result.put("code", 200);
            result.put("message", content);

        }

        return result;
    }

    private BizTaxInstanceCategoryPersonalPayrollItem checkObject(
        BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem,
        String optType) {

        if (null == bizTaxInstanceCategoryPersonalPayrollItem) {

            throw BizTaxException
                .throwException(BizTaxException.Codes.BizTaxException, "传递的数据对象为空!");

        }

        CheckClass checkClass = JSON
            .parseObject(JSON.toJSONString(bizTaxInstanceCategoryPersonalPayrollItem),
                CheckClass.class);

        try {

            Map resultMap = (Map) checkObjFieldIsNull(checkClass);

            if ((Boolean) resultMap.get("flag")) {

                List errorList = (List) resultMap.get("error");
                throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException,
                    "传递的数据参数不正确!" + errorList);

            }


        } catch (IllegalAccessException e) {

            e.printStackTrace();

            throw BizTaxException
                .throwException(BizTaxException.Codes.BizTaxException, "数据转化出错,请重试!");

        }

        bizTaxInstanceCategoryPersonalPayrollItem
            .setUseType(BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare);

        if (optType.equals(DataOperationType.CREATE)) {

            bizTaxInstanceCategoryPersonalPayrollItem.setSort(1);

        }

        BizMdDictionaryCode taxOfficeBizMdDictionaryCodes = bizMdDictionaryCodeService
            .findByDefsnAndSn("countrytype",
                bizTaxInstanceCategoryPersonalPayrollItem.getCountryid());

        if (StringUtils.isEmpty(taxOfficeBizMdDictionaryCodes)) {

            throw BizTaxException
                .throwException(BizTaxException.Codes.BizTaxException, "传递的国家编码错误!");

        }

        bizTaxInstanceCategoryPersonalPayrollItem
            .setCountryname(taxOfficeBizMdDictionaryCodes.getName());

        return bizTaxInstanceCategoryPersonalPayrollItem;

    }


    @Override
    public Map<String, Object> downItemFromTaxCheck(long taxInstanceCategoryId) {

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(taxInstanceCategoryId);
        int code = 0;
        String msg = "校验成功";
        if (bizTaxInstanceCategory == null) {
            code = -1;
            msg = "公司不存在";
        }
        if (!BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState())) {
            code = 1;
            msg = "状态已不为发起状态";
        }
        if (BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            code = 2;
            msg = "正在处理其他任务,稍后再试";
        }
        if (!DeclareType.none.equals(bizTaxInstanceCategory.getDeclareType())) {
            code = 3;
            msg = "已申报,不可下载";
        }
        if (bizTaxInstanceCategory.getAudit() != 0) {
            code = 4;
            msg = "已审核";
        }
        int finalCode = code;
        String finalMsg = msg;
        Map<String, Object> backMap = new HashMap<String, Object>() {{
            put("code", finalCode);
            put("msg", finalMsg);
        }};
        return backMap;
    }

    private Object checkObjFieldIsNull(Object obj) throws IllegalAccessException {

        Map<String, Object> result = new HashMap();

        List<String> error = new ArrayList<>();

        boolean flag_1 = false;

        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            logger.info(f.getName());
            if (f.get(obj) == null || f.get(obj).equals("")) {

                flag_1 = true;

                switch (f.getName()) {

                    case "wage":
                        error.add("收入额为空");
                        break;
                    case "dutyfreeamount":
                        error.add("免税所得为空");
                        break;
                    case "pension":
                        error.add("基本养老费为空");
                        break;
                    case "healthinsurance":
                        error.add("基本医保费为空");
                        break;
                    case "unemploymentinsurance":
                        error.add("失业保险费为空");
                        break;
                    case "housingfund":
                        error.add("住房公积金为空");
                        break;
                    case "originalproperty":
                        error.add("财产原值为空");
                        break;
                    case "allowdeduction":
                        error.add("允许扣除的税费为空");
                        break;
                    case "other":
                        error.add("其他为空");
                        break;
                    case "total":
                        error.add("合计为空");
                        break;
                    case "deductionamount":
                        error.add("减除费用为空");
                        break;
                    case "deductiondonate":
                        error.add("准予扣除的捐赠额为空");
                        break;
                    case "taxincome":
                        error.add("应纳税所得额为空");
                        break;
                    case "taxrate":
                        error.add("税率为空");
                        break;
                    case "speeddeduction":
                        error.add("速算扣除数为空");
                        break;
                    case "shouldpaytax":
                        error.add("应纳税额为空");
                        break;
                    case "relieftax":
                        error.add("减免税额为空");
                        break;
                    case "shouldcosttax":
                        error.add("应扣缴税额为空");
                        break;
                    case "alreadycosttax":
                        error.add("已扣缴税额为空");
                        break;
                    case "finallytax":
                        error.add("应补（退）税额为空");
                        break;
                    case "deduction":
                        error.add("投资抵扣为空");
                        break;
                    case "annuity":
                        error.add("年金为空");
                        break;
                    case "insurance":
                        error.add("商业健康险为空");
                        break;
                    case "alreadydeclarewage":
                        error.add("已申报金额为空");
                        break;
                    case "taxwage":
                        error.add("含税收入额为空");
                        break;
                }
            }
        }

        result.put("flag", flag_1);
        result.put("error", error);

        return result;
    }

    public static class CheckClass {

        private long bizTaxInstanceCategoryPersonalPayrollId;

        //private String useType = "declare";//用途[declare申报][result申报结果]

        //private long sort = 1;//序号

        private BigDecimal wage;//收入额

        private BigDecimal dutyfreeamount;//免税所得

        private BigDecimal pension;//基本养老费

        private BigDecimal healthinsurance;//基本医保费

        private BigDecimal unemploymentinsurance;//失业保险费

        private BigDecimal housingfund;//住房公积金

        private BigDecimal originalproperty;//财产原值

        private BigDecimal allowdeduction;//允许扣除的税费

        private BigDecimal other;//其他

        private BigDecimal total;//合计

        private BigDecimal deductionamount;//减除费用

        private BigDecimal deductiondonate;//准予扣除的捐赠额

        private BigDecimal taxincome;//应纳税所得额

        private BigDecimal taxrate;//税率

        private BigDecimal speeddeduction;//速算扣除数

        private BigDecimal shouldpaytax;//应纳税额

        private BigDecimal relieftax;//减免税额

        private BigDecimal shouldcosttax;//应扣缴税额

        private BigDecimal alreadycosttax;//已扣缴税额

        private BigDecimal finallytax;//应补（退）税额

        private BigDecimal deduction;//投资抵扣

        private BigDecimal annuity;//年金

        private BigDecimal insurance;//商业健康险

        private BigDecimal alreadydeclarewage;//已申报金额

        private BigDecimal taxwage;//含税收入额

        public long getBizTaxInstanceCategoryPersonalPayrollId() {
            return bizTaxInstanceCategoryPersonalPayrollId;
        }

        public void setBizTaxInstanceCategoryPersonalPayrollId(
            long bizTaxInstanceCategoryPersonalPayrollId) {
            this.bizTaxInstanceCategoryPersonalPayrollId = bizTaxInstanceCategoryPersonalPayrollId;
        }

        public BigDecimal getWage() {
            return wage;
        }

        public void setWage(BigDecimal wage) {
            this.wage = wage;
        }

        public BigDecimal getDutyfreeamount() {
            return dutyfreeamount;
        }

        public void setDutyfreeamount(BigDecimal dutyfreeamount) {
            this.dutyfreeamount = dutyfreeamount;
        }

        public BigDecimal getPension() {
            return pension;
        }

        public void setPension(BigDecimal pension) {
            this.pension = pension;
        }

        public BigDecimal getHealthinsurance() {
            return healthinsurance;
        }

        public void setHealthinsurance(BigDecimal healthinsurance) {
            this.healthinsurance = healthinsurance;
        }

        public BigDecimal getUnemploymentinsurance() {
            return unemploymentinsurance;
        }

        public void setUnemploymentinsurance(BigDecimal unemploymentinsurance) {
            this.unemploymentinsurance = unemploymentinsurance;
        }

        public BigDecimal getHousingfund() {
            return housingfund;
        }

        public void setHousingfund(BigDecimal housingfund) {
            this.housingfund = housingfund;
        }

        public BigDecimal getOriginalproperty() {
            return originalproperty;
        }

        public void setOriginalproperty(BigDecimal originalproperty) {
            this.originalproperty = originalproperty;
        }

        public BigDecimal getAllowdeduction() {
            return allowdeduction;
        }

        public void setAllowdeduction(BigDecimal allowdeduction) {
            this.allowdeduction = allowdeduction;
        }

        public BigDecimal getOther() {
            return other;
        }

        public void setOther(BigDecimal other) {
            this.other = other;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }

        public BigDecimal getDeductionamount() {
            return deductionamount;
        }

        public void setDeductionamount(BigDecimal deductionamount) {
            this.deductionamount = deductionamount;
        }

        public BigDecimal getDeductiondonate() {
            return deductiondonate;
        }

        public void setDeductiondonate(BigDecimal deductiondonate) {
            this.deductiondonate = deductiondonate;
        }

        public BigDecimal getTaxincome() {
            return taxincome;
        }

        public void setTaxincome(BigDecimal taxincome) {
            this.taxincome = taxincome;
        }

        public BigDecimal getTaxrate() {
            return taxrate;
        }

        public void setTaxrate(BigDecimal taxrate) {
            this.taxrate = taxrate;
        }

        public BigDecimal getSpeeddeduction() {
            return speeddeduction;
        }

        public void setSpeeddeduction(BigDecimal speeddeduction) {
            this.speeddeduction = speeddeduction;
        }

        public BigDecimal getShouldpaytax() {
            return shouldpaytax;
        }

        public void setShouldpaytax(BigDecimal shouldpaytax) {
            this.shouldpaytax = shouldpaytax;
        }

        public BigDecimal getRelieftax() {
            return relieftax;
        }

        public void setRelieftax(BigDecimal relieftax) {
            this.relieftax = relieftax;
        }

        public BigDecimal getShouldcosttax() {
            return shouldcosttax;
        }

        public void setShouldcosttax(BigDecimal shouldcosttax) {
            this.shouldcosttax = shouldcosttax;
        }

        public BigDecimal getAlreadycosttax() {
            return alreadycosttax;
        }

        public void setAlreadycosttax(BigDecimal alreadycosttax) {
            this.alreadycosttax = alreadycosttax;
        }

        public BigDecimal getFinallytax() {
            return finallytax;
        }

        public void setFinallytax(BigDecimal finallytax) {
            this.finallytax = finallytax;
        }

        public BigDecimal getDeduction() {
            return deduction;
        }

        public void setDeduction(BigDecimal deduction) {
            this.deduction = deduction;
        }

        public BigDecimal getAnnuity() {
            return annuity;
        }

        public void setAnnuity(BigDecimal annuity) {
            this.annuity = annuity;
        }

        public BigDecimal getInsurance() {
            return insurance;
        }

        public void setInsurance(BigDecimal insurance) {
            this.insurance = insurance;
        }

        public BigDecimal getAlreadydeclarewage() {
            return alreadydeclarewage;
        }

        public void setAlreadydeclarewage(BigDecimal alreadydeclarewage) {
            this.alreadydeclarewage = alreadydeclarewage;
        }

        public BigDecimal getTaxwage() {
            return taxwage;
        }

        public void setTaxwage(BigDecimal taxwage) {
            this.taxwage = taxwage;
        }
    }
}
