package com.yun9.service.tax.core.dto;

import com.yun9.biz.bill.domain.enums.Category;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.utils.FileParse;
import lombok.Data;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

@Data
public class BizTaxBusinessSheetDTO {
    public enum ERROR {
        error, warning, success;
    }

    public enum TaxDeadline {
        MONTH("m", "月"),
        QUARTER("q", "季");
        String name;
        String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(java.lang.String message) {
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        TaxDeadline(String name, String message) {
            this.name = name;
            this.message = message;
        }

        public static BizTaxBusinessSheetDTO.TaxDeadline getName(String name) {
            for (BizTaxBusinessSheetDTO.TaxDeadline d : BizTaxBusinessSheetDTO.TaxDeadline.values()) {
                if (d.getName().equals(name)) {
                    return d;
                }
            }
            return MONTH;
        }

        public static BizTaxBusinessSheetDTO.TaxDeadline getMessage(String message) {
            for (BizTaxBusinessSheetDTO.TaxDeadline d : BizTaxBusinessSheetDTO.TaxDeadline.values()) {
                if (d.getMessage().equals(message)) {
                    return d;
                }
            }
            return MONTH;
        }
    }

    /**
     * 公司名称
     */
    String mdCompanyName;

    String taxDeadlineStr;
    /**
     * 总收入
     */
    BigDecimal incomeAmount;
    String incomeAmountStr;
    /**
     * 总成本
     */
    BigDecimal buyAmount;
    String buyAmountStr;

    /**
     * 减除费用
     */
    BigDecimal deductionAmount;
    String deductionAmountStr;

    Integer row;

    String error;
    ERROR errorType;

    Long mdCompanyId;
    Long taxInstanceId;
    Long instanceCategoryId;
    Long instanceCategoryBusinessId;
    Long mdAccountCycleId;


    public static List<BizTaxBusinessSheetDTO> buildSheet(File file, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer beginRow, Integer colNum) {

        Map<Integer, List<ExcelParseDTO>> datas = FileParse.parse(file, map, beginRow, colNum);
        LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> maps = BizBusinessExcelDTO.build();
        List<BizTaxBusinessSheetDTO> auditSheet = new ArrayList<>();
        if (datas.size() != 0) {
            for (int i = beginRow; i < datas.size() + beginRow; i++) {
                List<ExcelParseDTO> excelParseDTOS = datas.get(i);
                if (CollectionUtils.isEmpty(excelParseDTOS)) {
                    break;
                }
                BizTaxBusinessSheetDTO bizTaxBusinessSheetDTO = new BizTaxBusinessSheetDTO();
                for (int j = 0; j < excelParseDTOS.size(); j++) {
                    ExcelParseDTO excelParseDTO = excelParseDTOS.get(j);
                    switch (j) {
                        case 0:
                            bizTaxBusinessSheetDTO.setMdCompanyName(excelParseDTO.getColValue());
                            break;
                        case 1:
                            bizTaxBusinessSheetDTO.setTaxDeadlineStr(excelParseDTO.getColValue());
                            break;
                        case 2:
                            bizTaxBusinessSheetDTO.setIncomeAmountStr(excelParseDTO.getColValue());
                            break;
                        case 3:
                            bizTaxBusinessSheetDTO.setBuyAmountStr(excelParseDTO.getColValue());
                            break;
                        case 4:
                            bizTaxBusinessSheetDTO.setDeductionAmountStr(excelParseDTO.getColValue());
                            break;
                        default:
                            break;
                    }
                }
                bizTaxBusinessSheetDTO.setRow(i);
                auditSheet.add(bizTaxBusinessSheetDTO);
            }
        }

        return auditSheet;
    }
}
