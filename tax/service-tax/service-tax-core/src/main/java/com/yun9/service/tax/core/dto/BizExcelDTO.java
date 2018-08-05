package com.yun9.service.tax.core.dto;

import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.bill.domain.enums.Category;
import lombok.Data;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Data
public class BizExcelDTO extends ExcelDTO {
    ;
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

    /**
     * 税率*100
     */
    Integer taxRate;

    public enum FiledName {
        mdCompanyName, mdAccountCycle, billType, cargo, service;
    }

    public BizExcelDTO() {
        super();
        this.setFontHeightInPoints((short) 10);
        this.setBackgroundColor(IndexedColors.YELLOW1.getIndex());
    }

    public static LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO >> build() {
        LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO >> map = new LinkedHashMap<>();
        LinkedHashMap<Integer, BizExcelDTO> oneRow = new LinkedHashMap<Integer, BizExcelDTO>();
        BizExcelDTO bizExcelDTO = new BizExcelDTO();
        bizExcelDTO.setName("填表说明：\r\n1.客户名称必填且不可重复，可直接使用导出的模板数据\r\n2.季度（月）指票据发生所在月份，必须是季度范围内的年月\r\n3.参考纳税人类型（非必填，仅作参考）填写票据数据，属于需要填写类型的票据，若无发生额，可不填");


        bizExcelDTO.setFontHeightInPoints((short) 10);
        bizExcelDTO.setFontColor(IndexedColors.RED.getIndex());
        bizExcelDTO.setBackgroundColor(null);
        oneRow.put(0, bizExcelDTO);
        map.put(0, oneRow);

        LinkedHashMap<Integer, BizExcelDTO> twoRow = new LinkedHashMap<Integer, BizExcelDTO>();
        //公司信息
        BizExcelDTO company = new BizExcelDTO();
        company.setName("客户名称(必填)");
        company.setFiled(FiledName.mdCompanyName.toString());
        company.setComment("填写说明:\r\n填写客户全称，请勿重复");
        twoRow.put(0, company);

        BizExcelDTO accountCycle = new BizExcelDTO();
        accountCycle.setName("季度(月)(必填)");
        accountCycle.setFiled(FiledName.mdAccountCycle.toString());
        accountCycle.setComment("填写说明:\r\n必须是季度范围内的年月");
        twoRow.put(1, accountCycle);

        BizExcelDTO companyType = new BizExcelDTO();
        companyType.setName("纳税人类型(选填)");
        companyType.setFiled(FiledName.billType.toString());
        companyType.setComment("填写说明:\r\n1:请保持与对应客户的增值税纳税人类型一致\r\n2.非必填,仅作参考");
        twoRow.put(2, companyType);

        BizExcelDTO cargoNoBill = new BizExcelDTO();
        cargoNoBill.setName("不开票收入\r\n劳务3%(选填)");
        cargoNoBill.setFiled(FiledName.cargo.toString());
        cargoNoBill.setComment("填写说明:\r\n1.“服务类”征收项目类型时不填,下载模板会默认为“--”\r\n2.实际无金额时不填");
        cargoNoBill.setBillType(BizBillInvoice.BillType.nobill.getValue());
        cargoNoBill.setCategory(Category.cargo.getValue());
        cargoNoBill.setTaxRate(3);
        twoRow.put(3, cargoNoBill);


        BizExcelDTO serviceNoBill = new BizExcelDTO();
        serviceNoBill.setName("不开票收入-服务(选填)");
        serviceNoBill.setComment("填写说明:\r\n1.“劳务类”征收项目类型时不填,下载模板会默认为“--”\r\n2.实际无金额时不填");
        twoRow.put(4, serviceNoBill);


        BizExcelDTO serviceOutputBizExcelDTO = new BizExcelDTO();
        serviceOutputBizExcelDTO.setName("自开普票-劳务(选填)");
        serviceOutputBizExcelDTO.setComment("填写说明:\r\n1.“服务类”征收项目类型时不填，下载模板会默认为“--”\r\n2.实际无金额时不填\n");
        twoRow.put(6, serviceOutputBizExcelDTO);


        BizExcelDTO serviceServiceOutputBizExcelDTO = new BizExcelDTO();
        serviceServiceOutputBizExcelDTO.setName("自开普票-服务(选填)");
        serviceServiceOutputBizExcelDTO.setComment("填写说明:\r\n1.1.“劳务类”征收项目类型时不填，下载模板会默认为“--”\r\n2.实际无金额时不填\n");
        twoRow.put(8, serviceServiceOutputBizExcelDTO);

        BizExcelDTO cargoCargoOutputBizExcelDTO = new BizExcelDTO();
        cargoCargoOutputBizExcelDTO.setName("自开专票\r\n劳务3%(选填)");
        cargoCargoOutputBizExcelDTO.setFiled(FiledName.cargo.toString());
        cargoCargoOutputBizExcelDTO.setComment("填写说明:\r\n1.1.“服务类”征收项目类型时不填，下载模板会默认为“--”\r\n2.实际无金额时不填\n");
        cargoCargoOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        cargoCargoOutputBizExcelDTO.setCategory(Category.cargo.getValue());
        cargoCargoOutputBizExcelDTO.setType(BizBillInvoice.Type.special.getValue());
        cargoCargoOutputBizExcelDTO.setTaxRate(3);
        twoRow.put(11, cargoCargoOutputBizExcelDTO);


        BizExcelDTO serviceServiceSpecialOutputBizExcelDTO = new BizExcelDTO();
        serviceServiceSpecialOutputBizExcelDTO.setName("自开专票-服务(选填)");
        serviceServiceSpecialOutputBizExcelDTO.setComment("填写说明:\r\n1.1.“劳务类”征收项目类型时不填，下载模板会默认为“--”\r\n2.实际无金额时不填\n");
        twoRow.put(12, serviceServiceSpecialOutputBizExcelDTO);
        map.put(1, twoRow);

        LinkedHashMap<Integer, BizExcelDTO> threeRow = new LinkedHashMap<Integer, BizExcelDTO>();

        BizExcelDTO serviceNoBillThree = new BizExcelDTO();
        serviceNoBillThree.setName("3%");
        serviceNoBillThree.setFiled(FiledName.service.toString());
        serviceNoBillThree.setBillType(BizBillInvoice.BillType.nobill.getValue());
        serviceNoBillThree.setCategory(Category.service.getValue());
        serviceNoBillThree.setTaxRate(3);
        threeRow.put(4, serviceNoBillThree);

        BizExcelDTO serviceNoBillFive = new BizExcelDTO();
        serviceNoBillFive.setName("5%");
        serviceNoBillFive.setFiled(FiledName.service.toString());
        serviceNoBillFive.setBillType(BizBillInvoice.BillType.nobill.getValue());
        serviceNoBillFive.setCategory(Category.service.getValue());
        serviceNoBillFive.setTaxRate(5);
        threeRow.put(5, serviceNoBillFive);

        BizExcelDTO cargoBillZero = new BizExcelDTO();
        cargoBillZero.setName("0%(出口免税发票)");
        cargoBillZero.setFiled(FiledName.cargo.toString());
        cargoBillZero.setBillType(BizBillInvoice.BillType.output.getValue());
        cargoBillZero.setCategory(Category.cargo.getValue());
        cargoBillZero.setType(BizBillInvoice.Type.plain.getValue());
        cargoBillZero.setTaxRate(0);
        threeRow.put(6, cargoBillZero);

        BizExcelDTO cargoBillThree = new BizExcelDTO();
        cargoBillThree.setName("3%");
        cargoBillThree.setFiled(FiledName.cargo.toString());
        cargoBillThree.setBillType(BizBillInvoice.BillType.output.getValue());
        cargoBillThree.setCategory(Category.cargo.getValue());
        cargoBillThree.setType(BizBillInvoice.Type.plain.getValue());
        cargoBillThree.setTaxRate(3);
        threeRow.put(7, cargoBillThree);

        BizExcelDTO serviceBillZero = new BizExcelDTO();
        serviceBillZero.setName("0%(出口免税发票)");
        serviceBillZero.setFiled(FiledName.service.toString());
        serviceBillZero.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceBillZero.setCategory(Category.service.getValue());
        serviceBillZero.setType(BizBillInvoice.Type.plain.getValue());
        serviceBillZero.setTaxRate(0);
        threeRow.put(8, serviceBillZero);

        BizExcelDTO serviceBillThree = new BizExcelDTO();
        serviceBillThree.setName("3%");
        serviceBillThree.setFiled(FiledName.service.toString());
        serviceBillThree.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceBillThree.setCategory(Category.service.getValue());
        serviceBillThree.setType(BizBillInvoice.Type.plain.getValue());
        serviceBillThree.setTaxRate(3);
        threeRow.put(9, serviceBillThree);

        BizExcelDTO serviceBillFive = new BizExcelDTO();
        serviceBillFive.setName("5%");
        serviceBillFive.setFiled(FiledName.service.toString());
        serviceBillFive.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceBillFive.setCategory(Category.service.getValue());
        serviceBillFive.setType(BizBillInvoice.Type.plain.getValue());
        serviceBillFive.setTaxRate(5);
        threeRow.put(10, serviceBillFive);


        BizExcelDTO serviceSpecialBillThree = new BizExcelDTO();
        serviceSpecialBillThree.setName("3%");
        serviceSpecialBillThree.setFiled(FiledName.service.toString());
        serviceSpecialBillThree.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceSpecialBillThree.setCategory(Category.service.getValue());
        serviceSpecialBillThree.setType(BizBillInvoice.Type.special.getValue());
        serviceSpecialBillThree.setTaxRate(3);
        threeRow.put(12, serviceSpecialBillThree);

        BizExcelDTO serviceSpecialBillFive = new BizExcelDTO();
        serviceSpecialBillFive.setName("5%");
        serviceSpecialBillFive.setFiled(FiledName.service.toString());
        serviceSpecialBillFive.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceSpecialBillFive.setCategory(Category.service.getValue());
        serviceSpecialBillFive.setType(BizBillInvoice.Type.special.getValue());
        serviceSpecialBillFive.setTaxRate(5);
        threeRow.put(13, serviceSpecialBillFive);
        map.put(2, threeRow);
        return map;
    }
    public static List<ExcelMergedDTO> bulidExportFileDTOs(){
        List<ExcelMergedDTO> excelMergedDTOS = new ArrayList<>();
        ExcelMergedDTO excelMergedDTO = new ExcelMergedDTO();
        excelMergedDTO.setMergedBeginRow(0);
        excelMergedDTO.setMergedEndRow(0);
        excelMergedDTO.setMergedBeginCol(0);
        excelMergedDTO.setMergedEndCol(13);
        excelMergedDTOS.add(excelMergedDTO);

        ExcelMergedDTO company = new ExcelMergedDTO();
        company.setMergedBeginRow(1);
        company.setMergedEndRow(2);
        company.setMergedBeginCol(0);
        company.setMergedEndCol(0);
        excelMergedDTOS.add(company);

        ExcelMergedDTO cycle = new ExcelMergedDTO();
        cycle.setMergedBeginRow(1);
        cycle.setMergedEndRow(2);
        cycle.setMergedBeginCol(1);
        cycle.setMergedEndCol(1);
        excelMergedDTOS.add(cycle);

        ExcelMergedDTO billType = new ExcelMergedDTO();
        billType.setMergedBeginRow(1);
        billType.setMergedEndRow(2);
        billType.setMergedBeginCol(2);
        billType.setMergedEndCol(2);
        excelMergedDTOS.add(billType);

        ExcelMergedDTO nobill = new ExcelMergedDTO();
        nobill.setMergedBeginRow(1);
        nobill.setMergedEndRow(2);
        nobill.setMergedBeginCol(3);
        nobill.setMergedEndCol(3);
        excelMergedDTOS.add(nobill);

        ExcelMergedDTO serviceNoBill = new ExcelMergedDTO();
        serviceNoBill.setMergedBeginRow(1);
        serviceNoBill.setMergedEndRow(1);
        serviceNoBill.setMergedBeginCol(4);
        serviceNoBill.setMergedEndCol(5);
        excelMergedDTOS.add(serviceNoBill);

        ExcelMergedDTO cargoNoBill = new ExcelMergedDTO();
        cargoNoBill.setMergedBeginRow(1);
        cargoNoBill.setMergedEndRow(1);
        cargoNoBill.setMergedBeginCol(6);
        cargoNoBill.setMergedEndCol(7);
        excelMergedDTOS.add(cargoNoBill);

        ExcelMergedDTO serviceBill = new ExcelMergedDTO();
        serviceBill.setMergedBeginRow(1);
        serviceBill.setMergedEndRow(1);
        serviceBill.setMergedBeginCol(8);
        serviceBill.setMergedEndCol(10);
        excelMergedDTOS.add(serviceBill);

        ExcelMergedDTO billS = new ExcelMergedDTO();
        billS.setMergedBeginRow(1);
        billS.setMergedEndRow(2);
        billS.setMergedBeginCol(11);
        billS.setMergedEndCol(11);
        excelMergedDTOS.add(billS);

        ExcelMergedDTO serviceBillS = new ExcelMergedDTO();
        serviceBillS.setMergedBeginRow(1);
        serviceBillS.setMergedEndRow(1);
        serviceBillS.setMergedBeginCol(12);
        serviceBillS.setMergedEndCol(13);
        excelMergedDTOS.add(serviceBillS);

        return excelMergedDTOS;
    }


    public static LinkedHashMap<Integer, BizExcelDTO> buildParse() {
        LinkedHashMap<Integer, BizExcelDTO> map = new LinkedHashMap<Integer, BizExcelDTO>();
        //公司信息
        BizExcelDTO bizExcelDTO = new BizExcelDTO();
        bizExcelDTO.setName("客户名称");
        map.put(0,bizExcelDTO);

        BizExcelDTO cycleBizExcelDTO = new BizExcelDTO();
        cycleBizExcelDTO.setName("季度(月)");
        map.put(1,cycleBizExcelDTO);

        BizExcelDTO billTypeBizExcelDTO = new BizExcelDTO();
        billTypeBizExcelDTO.setName("征税项目类型");
        map.put(2,billTypeBizExcelDTO);

        BizExcelDTO cargoBizExcelDTO = new BizExcelDTO();
        cargoBizExcelDTO.setName("不开票收入\r\n(劳务3%)");
        cargoBizExcelDTO.setBillType(BizBillInvoice.BillType.nobill.getValue());
        cargoBizExcelDTO.setCategory(Category.cargo.getValue());
        cargoBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        cargoBizExcelDTO.setTaxRate(3);
        map.put(3,cargoBizExcelDTO);

        BizExcelDTO serviceBizExcelDTO = new BizExcelDTO();
        serviceBizExcelDTO.setName("不开票收入\r\n(服务3%)");
        serviceBizExcelDTO.setBillType(BizBillInvoice.BillType.nobill.getValue());
        serviceBizExcelDTO.setCategory(Category.service.getValue());
        serviceBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceBizExcelDTO.setTaxRate(3);
        map.put(4,serviceBizExcelDTO);

        BizExcelDTO serviceNobillBizExcelDTO = new BizExcelDTO();
        serviceNobillBizExcelDTO.setName("不开票收入\r\n(服务5%)");
        serviceNobillBizExcelDTO.setBillType(BizBillInvoice.BillType.nobill.getValue());
        serviceNobillBizExcelDTO.setCategory(Category.service.getValue());
        serviceNobillBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceNobillBizExcelDTO.setTaxRate(5);
        map.put(5,serviceNobillBizExcelDTO);

        BizExcelDTO serviceOutputBizExcelDTO = new BizExcelDTO();
        serviceOutputBizExcelDTO.setName("自开普票\r\n(劳务0%(出口免税发票))");
        serviceOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceOutputBizExcelDTO.setCategory(Category.cargo.getValue());
        serviceOutputBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceOutputBizExcelDTO.setTaxRate(0);
        map.put(6,serviceOutputBizExcelDTO);

        BizExcelDTO serviceCargoOutputBizExcelDTO = new BizExcelDTO();
        serviceCargoOutputBizExcelDTO.setName("自开普票\r\n(劳务3%)");
        serviceCargoOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceCargoOutputBizExcelDTO.setCategory(Category.cargo.getValue());
        serviceCargoOutputBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceCargoOutputBizExcelDTO.setTaxRate(3);
        map.put(7,serviceCargoOutputBizExcelDTO);



        BizExcelDTO serviceServiceOutputBizExcelDTO = new BizExcelDTO();
        serviceServiceOutputBizExcelDTO.setName("自开普票\r\n(服务0%(出口免税发票))");
        serviceServiceOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceServiceOutputBizExcelDTO.setCategory(Category.service.getValue());
        serviceServiceOutputBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceServiceOutputBizExcelDTO.setTaxRate(0);
        map.put(8,serviceServiceOutputBizExcelDTO);

        BizExcelDTO serviceServiceplainOutputBizExcelDTO = new BizExcelDTO();
        serviceServiceplainOutputBizExcelDTO.setName("自开普票\r\n(服务3%)");
        serviceServiceplainOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceServiceplainOutputBizExcelDTO.setCategory(Category.service.getValue());
        serviceServiceplainOutputBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceServiceplainOutputBizExcelDTO.setTaxRate(3);
        map.put(9,serviceServiceplainOutputBizExcelDTO);

        BizExcelDTO serviceServiceOutputplainBizExcelDTO = new BizExcelDTO();
        serviceServiceOutputplainBizExcelDTO.setName("自开普票\r\n(服务5%)");
        serviceServiceOutputplainBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceServiceOutputplainBizExcelDTO.setCategory(Category.service.getValue());
        serviceServiceOutputplainBizExcelDTO.setType(BizBillInvoice.Type.plain.getValue());
        serviceServiceOutputplainBizExcelDTO.setTaxRate(5);
        map.put(10,serviceServiceOutputplainBizExcelDTO);

        BizExcelDTO cargoCargoOutputBizExcelDTO = new BizExcelDTO();
        cargoCargoOutputBizExcelDTO.setName("自开专票\r\n(劳务3%)");
        cargoCargoOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        cargoCargoOutputBizExcelDTO.setCategory(Category.cargo.getValue());
        cargoCargoOutputBizExcelDTO.setType(BizBillInvoice.Type.special.getValue());
        cargoCargoOutputBizExcelDTO.setTaxRate(3);
        map.put(11,cargoCargoOutputBizExcelDTO);

        BizExcelDTO serviceServiceSpecialOutputBizExcelDTO = new BizExcelDTO();
        serviceServiceSpecialOutputBizExcelDTO.setName("自开专票\r\n(服务3%)");
        serviceServiceSpecialOutputBizExcelDTO.setFiled("service");
        serviceServiceSpecialOutputBizExcelDTO.setComment("填写说明:\r\n1.1.“劳务类”征收项目类型时不填，下载模板会默认为“--”\r\n2.实际无金额时不填\n");
        serviceServiceSpecialOutputBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceServiceSpecialOutputBizExcelDTO.setCategory(Category.service.getValue());
        serviceServiceSpecialOutputBizExcelDTO.setType(BizBillInvoice.Type.special.getValue());
        serviceServiceSpecialOutputBizExcelDTO.setTaxRate(3);
        map.put(12,serviceServiceSpecialOutputBizExcelDTO);

        BizExcelDTO serviceServiceOutputSpecialBizExcelDTO = new BizExcelDTO();
        serviceServiceOutputSpecialBizExcelDTO.setName("自开专票\r\n(服务5%)");
        serviceServiceOutputSpecialBizExcelDTO.setBillType(BizBillInvoice.BillType.output.getValue());
        serviceServiceOutputSpecialBizExcelDTO.setCategory(Category.service.getValue());
        serviceServiceOutputSpecialBizExcelDTO.setType(BizBillInvoice.Type.special.getValue());
        serviceServiceOutputSpecialBizExcelDTO.setTaxRate(5);
        map.put(13,serviceServiceOutputSpecialBizExcelDTO);
        return map;
    }
}
