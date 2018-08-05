package com.yun9.service.tax.core.dto;

import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.utils.FileParse;
import lombok.Data;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

@Data
public class BizTaxBillInvoiceSheetDTO {
    public enum ERROR {
        error, warning, success;
    }

    String mdCompanyName;
    String companyType;
    String mdAccountCycle;
    /**
     * 票据类型[output自开][agent代开][nobill无票][income进项]',
     */
    String billType;
    /**
     * 开票类型[service服务][cargo劳务]
     */
    String category;
    /**
     * 发票类型[special专票][plain普票]
     */
    String type;
    BigDecimal taxRate;
    String declareAmount;
    BigDecimal amount;
    String colName;
    Integer col;
    String error;
    ERROR errorType;

    Long mdCompanyId;
    Long taxInstanceId;
    Long instanceCategoryId;
    Long instanceCategoryVatQId;
    Long mdAccountCycleId;

    public static void build(List<BizTaxBillInvoiceSheetDTO> list, BizTaxBillInvoiceSheetDTO bizTaxBillInvoiceSheetDTO, LinkedHashMap<Integer, BizExcelDTO> map) {
        BizExcelDTO bizExcelDTO =  map.get(bizTaxBillInvoiceSheetDTO.getCol());
        bizTaxBillInvoiceSheetDTO.setBillType(bizExcelDTO.getBillType());
        bizTaxBillInvoiceSheetDTO.setTaxRate(new BigDecimal(bizExcelDTO.getTaxRate()).divide(new BigDecimal(100)));
        bizTaxBillInvoiceSheetDTO.setCategory(bizExcelDTO.getCategory());
        bizTaxBillInvoiceSheetDTO.setType(bizExcelDTO.getType());
        bizTaxBillInvoiceSheetDTO.setColName(bizExcelDTO.getName());
        list.add(bizTaxBillInvoiceSheetDTO);
    }

    public static Map<Integer, List<BizTaxBillInvoiceSheetDTO>> buildSheet(File file, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer beginRow, Integer colNum) {

        Map<Integer, List<ExcelParseDTO>> datas = FileParse.parse(file, map, beginRow, colNum);
        LinkedHashMap<Integer, BizExcelDTO> maps = BizExcelDTO.buildParse();
        Map<Integer, List<BizTaxBillInvoiceSheetDTO>> auditSheet = new HashMap<>();
        if (datas.size() != 0) {
            for (int i = beginRow; i < datas.size()+beginRow; i++) {
                List<ExcelParseDTO> excelParseDTOS = datas.get(i);
                if (CollectionUtils.isEmpty(excelParseDTOS)) {
                    break;
                }
                List<BizTaxBillInvoiceSheetDTO> listRow = new ArrayList<>();
                String companyName = null;
                String accountCycle = null;
                String companyType = null;
                for (int j = 0; j < excelParseDTOS.size(); j++) {
                    ExcelParseDTO excelParseDTO = excelParseDTOS.get(j);
                    if (j < 3) {
                        if (null != excelParseDTO) {
                            switch (j) {
                                case 0:
                                    companyName = FileParse.replaceSpecial(excelParseDTO.getColValue());
                                    break;
                                case 1:
                                    accountCycle = excelParseDTO.getColValue();
                                    break;
                                case 2:
                                    companyType = excelParseDTO.getColValue();
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else {
                        if (null != excelParseDTO) {
                            if (!"--".equals(excelParseDTO.getColValue()) && StringUtils.isNotEmpty(excelParseDTO.getColValue())) {
                                BizTaxBillInvoiceSheetDTO bizTaxBillInvoiceSheetDTO = new BizTaxBillInvoiceSheetDTO();
                                bizTaxBillInvoiceSheetDTO.setCol(excelParseDTO.getCol());
                                bizTaxBillInvoiceSheetDTO.setMdCompanyName(companyName);
                                bizTaxBillInvoiceSheetDTO.setCompanyType(companyType);
                                bizTaxBillInvoiceSheetDTO.setMdAccountCycle(accountCycle);
                                bizTaxBillInvoiceSheetDTO.setDeclareAmount(excelParseDTO.getColValue());
                                BizTaxBillInvoiceSheetDTO.build(listRow, bizTaxBillInvoiceSheetDTO, maps);
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(listRow)) {
                        auditSheet.put(i + 1, listRow);
                    }
                }
            }
        }

        return auditSheet;
    }

}
