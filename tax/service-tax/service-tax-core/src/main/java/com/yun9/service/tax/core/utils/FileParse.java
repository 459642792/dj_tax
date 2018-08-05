package com.yun9.service.tax.core.utils;

import com.opencsv.CSVReader;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.dto.ExcelDTO;
import com.yun9.service.tax.core.dto.ExcelParseDTO;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.impl.TaxInstanceCategoryPersonalPayrollItemFactoryImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileParse {

    public static final Logger logger = LoggerFactory.getLogger(FileParse.class);


    private TaxInstanceCategoryPersonalPayrollItemFactoryImpl taxInstanceCategoryPersonalPayrollItemFactory;

    private static final Pattern REPLACE_SPECIAL_PATTERN = Pattern.compile("\\s*|\t|\r|\n");

    /**
     * 解析数据
     *
     * @param file
     * @return
     */
    public static Map<Integer, List<ExcelParseDTO>> parse(File file, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer beginRow, Integer colNum) {
        logger.debug("开始解析文件：{}", file.getName());
        String fileType = file.getName().substring(file.getName().lastIndexOf("."));
        String type = StringUtils.isNotEmpty(fileType.substring(1)) ? fileType.substring(1) : null;
        logger.debug("文件名:{},文件类型:{}", file.getName(), type);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if ("csv".equals(type)) {
                return readCSV(inputStream, map, beginRow, colNum);
            } else {
                return parseExcel(type, inputStream, map, beginRow, colNum);
            }
        } catch (FileNotFoundException e) {
            logger.error("解析文件出现错误.", e);
            file.delete();
            throw BizTaxException.build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION, e);
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


    private static Map<Integer, List<ExcelParseDTO>> parseExcel(String fileType, InputStream inputStream, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer beginRow, Integer colNum) {
        Workbook wb = readExcel(fileType, inputStream);
        int sheetNumber = wb.getNumberOfSheets();
        List<Object> lists = new ArrayList<>();
        logger.debug("解析Excel文件，共找到{}张Sheet", sheetNumber);
        Map<Integer, List<ExcelParseDTO>> auditSheet = null;
        for (int i = 0; i < sheetNumber; i++) {
            //构建数据行
            auditSheet = buildSheet(wb.getSheetAt(i), map, beginRow, colNum);
        }
        return auditSheet;
    }


    /**
     * 得到Excel中Sheet的详细数据；
     *
     * @param sheet
     * @return
     */
    private static Map<Integer, List<ExcelParseDTO>> buildSheet(Sheet sheet, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer beginRow, Integer colNum) {
        Map<Integer, List<ExcelParseDTO>> auditSheet = new HashMap<>();
        int maxRow = sheet.getLastRowNum() + 1;
        logger.debug("解析Excel具体Sheet:{}，sheet共有{}行", sheet.getSheetName(), maxRow);
        for (int i = 0; i < maxRow; i++) {
            Row row = sheet.getRow(i);
            if (null == row) {
                break;
            }

            if (beginRow > i) {
                matchTemplate(map, row);
            } else {
//                int error = 0;
//                for (int j = 0; j <= colNum; j++) {
//                    Cell cell = row.getCell(j);
//                    if (null == cell || null == ((XSSFCell) cell).getRawValue() ){
//                        error ++;
//                    }
//                }
//                if (error == colNum+1){
//                    break;
//                }
                List<ExcelParseDTO> listRow = new ArrayList<>();
                for (int j = 0; j <= colNum; j++) {
                    Cell cell = row.getCell(j);
                    ExcelParseDTO excelParseDTO = new ExcelParseDTO();
                    excelParseDTO.setCol(j);
                    if (null != cell) {
                        cell.setCellType(CellType.STRING);
                        excelParseDTO.setColValue(cell.getStringCellValue());
                    } else {
                        excelParseDTO.setColValue(null);
                    }
                    listRow.add(excelParseDTO);
                }
                if (CollectionUtils.isNotEmpty(listRow)) {
                    auditSheet.put(i, listRow);
                }
            }

        }

        return auditSheet;
    }

    private static void matchTemplate(LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Row row) {
      logger.debug("开始解析表头");
        if (null == row) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR, "请按照下载的模板提交");
        }
        LinkedHashMap<Integer, ? extends ExcelDTO> maps = map.get(row.getRowNum());
        for (Integer col : maps.keySet()) {
            Cell cell = row.getCell(col);
            if (null == cell) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR ,"请按照下载的模板提交");
            }
            cell.setCellType(CellType.STRING);
            String cellValue = cell.getStringCellValue();
//            String cellValue = cell.getStringCellValue().replace("\n", "\r\n");

            logger.debug("开始解析表头数据==============={}",cellValue);
            if (!maps.get(col).getName().equals(cellValue)) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR ,"请按照下载的模板提交");
            }
        }
    }

    /**
     * 解析csv
     *
     * @param inputStream 流
     * @return
     */
    private static Map<Integer, List<ExcelParseDTO>> readCSV(InputStream inputStream, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer beginRow, Integer colNum) {
        Map<Integer, List<ExcelParseDTO>> auditSheet = new HashMap<>();
        CSVReader reader = null;
        try {
            reader = new CSVReader(new InputStreamReader(inputStream, "gbk"));
            String[] strArr = null;
            int i = 0;
            while ((strArr = reader.readNext()) != null) {
                if (beginRow > i) {
                    LinkedHashMap<Integer, ? extends ExcelDTO> maps = map.get(i);
                    for (Integer col : maps.keySet()) {
                        String cellValue = strArr[col].replace("\n", "\r\n");
                        ;
                        if (!map.get(i).get(col).getName().equals(cellValue)) {
                            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR, "请按照下载的模板提交");
                        }
                    }
                    i++;
                } else {
                    String str = "";
                    for (int j = 0; j <= colNum; j++) {
                        str += strArr[j];
                    }
                    if (!"--".equals(str.trim()) && !"".equals(str.trim())) {
                        List<ExcelParseDTO> listRow = new ArrayList<>();
                        for (int j = 0; j <= colNum; j++) {
                            ExcelParseDTO excelParseDTO = new ExcelParseDTO();
                            excelParseDTO.setCol(j);
                            excelParseDTO.setColValue(strArr[j]);
                            listRow.add(excelParseDTO);
                        }
                        auditSheet.put(i, listRow);
                        i++;
                    }
                }


            }
        } catch (Exception e) {
            logger.debug("POI解析Excel输入流异常", e);
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR, "解析csv异常,");
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
        return auditSheet;
    }


    public static Workbook readExcel(String fileType, InputStream inputStream) {
        Workbook wb = null;
        if (inputStream == null) {
            return null;
        }
        try {
            if ("xls".equals(fileType)) {
                return wb = new HSSFWorkbook(inputStream);
            } else if ("xlsx".equals(fileType) || "xlsm".equals(fileType)) {
                return wb = new XSSFWorkbook(inputStream);
            } else {
                throw BizTaxException.build(BizTaxException.Codes.WRONG_DATA_FORMAT, fileType);
            }
        } catch (Exception e) {
            logger.debug("POI解析Excel输入流异常", e);
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR ,"您的版本可能不兼容,请打开另存为csv格式,");
        }
    }



    public static String replaceSpecial(String value) {
        String repl = null;
        //去除字符串中的空格、回车、换行符、制表符等
        if (value != null) {
            Matcher m = REPLACE_SPECIAL_PATTERN.matcher(value);
            repl = m.replaceAll("");
        }
        return repl;
    }


}
