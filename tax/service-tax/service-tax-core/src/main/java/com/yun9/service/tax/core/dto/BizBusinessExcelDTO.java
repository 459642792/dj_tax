package com.yun9.service.tax.core.dto;

import lombok.Data;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Data
public class BizBusinessExcelDTO extends  ExcelDTO{
    public BizBusinessExcelDTO() {
        super();
        this.setFontHeightInPoints((short) 10);
        this.setBackgroundColor(IndexedColors.YELLOW1.getIndex());
    }


    public enum FiledName {
        mdCompanyName, taxDeadline, incomeAmount, buyAmount, deductionAmount;
    }
    public static  LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO >>  build() {
        LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO >> map = new LinkedHashMap<>();

        LinkedHashMap<Integer, BizBusinessExcelDTO> oneRow = new LinkedHashMap<Integer, BizBusinessExcelDTO>();
        BizBusinessExcelDTO bizExcelDTsO = new BizBusinessExcelDTO();
        bizExcelDTsO.setName("填表说明：\r\n" +
                "1.客户名称必填且不可重复，可直接使用导出的模板数据；\r\n" +
                "2.填报项无金额时，可不填，系统将自动按0.00导入；\r\n" +
                "3.总收入由该客户的增值税销售额生成（增值税数据已审核/已申报）；\r\n" +
                "4.总利润无需录入，系统通过公式计算得出；");
        bizExcelDTsO.setFontHeightInPoints((short) 10);
        bizExcelDTsO.setFontColor(IndexedColors.RED.getIndex());
        bizExcelDTsO.setBackgroundColor(null);
        oneRow.put(0, bizExcelDTsO);
        map.put(0, oneRow);

        LinkedHashMap<Integer, BizBusinessExcelDTO> twoRow = new LinkedHashMap<Integer, BizBusinessExcelDTO>();
        //公司信息
        BizBusinessExcelDTO bizExcelDTO = new BizBusinessExcelDTO();
        bizExcelDTO.setName("*客户名称");
        bizExcelDTO.setFiled( FiledName.mdCompanyName.toString());
        twoRow.put(0, bizExcelDTO);

        BizBusinessExcelDTO deadlineBizExcelDTO = new BizBusinessExcelDTO();
        deadlineBizExcelDTO.setName("纳税期限");
        deadlineBizExcelDTO.setFiled(FiledName.taxDeadline.toString());
        twoRow.put(1, deadlineBizExcelDTO);

        BizBusinessExcelDTO incomeBizExcelDTO = new BizBusinessExcelDTO();
        incomeBizExcelDTO.setName("*总收入");
        incomeBizExcelDTO.setFiled(FiledName.incomeAmount.toString());
        incomeBizExcelDTO.setComment("填写说明:\r\n1:总收入为必填项;\r\n2.若获取到该客户的增值税销售额,则导出利润核算模板时一起生成;\r\n3:实际无金额");
        twoRow.put(2, incomeBizExcelDTO);

        BizBusinessExcelDTO buyBizExcelDTO = new BizBusinessExcelDTO();
        buyBizExcelDTO.setName("*总成本");
        buyBizExcelDTO.setFiled(FiledName.buyAmount.toString());
        buyBizExcelDTO.setComment("填写说明:\r\n1.总成本为必填项;\r\n2.实际无金额时,总成本默认为0.00");
        twoRow.put(3, buyBizExcelDTO);

        BizBusinessExcelDTO deductionBizExcelDTO = new BizBusinessExcelDTO();
        deductionBizExcelDTO.setName("*减除费用");
        deductionBizExcelDTO.setFiled(FiledName.deductionAmount.toString());
        deductionBizExcelDTO.setComment("填写说明:\r\n1.减除费用为必填项;\r\n2.实际无金额时,总成本默认为0.00");
        twoRow.put(4, deductionBizExcelDTO);
        map.put(1, twoRow);
        return map;
    }
    public static List<ExcelMergedDTO> bulidExportFileDTOs(){
        List<ExcelMergedDTO> excelMergedDTOS = new ArrayList<>();
        ExcelMergedDTO excelMergedDTO = new ExcelMergedDTO();
        excelMergedDTO.setMergedBeginRow(0);
        excelMergedDTO.setMergedEndRow(0);
        excelMergedDTO.setMergedBeginCol(0);
        excelMergedDTO.setMergedEndCol(4);
        excelMergedDTOS.add(excelMergedDTO);

        return excelMergedDTOS;
    }
}
