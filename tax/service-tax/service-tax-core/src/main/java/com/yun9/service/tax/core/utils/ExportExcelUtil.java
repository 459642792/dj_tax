package com.yun9.service.tax.core.utils;

import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.dto.ExcelDTO;
import com.yun9.service.tax.core.dto.ExcelMergedDTO;
import com.yun9.service.tax.core.dto.ExportFileDTO;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExportExcelUtil implements Serializable {

    protected static Logger logger = LoggerFactory.getLogger(ExportExcelUtil.class);

    /**
     * 表头行样式
     */
    private static CellStyle headStyle;
    /**
     * 表头行字体
     */
    private static Font headFont;
    /**
     * 内容行样式
     */
    private static CellStyle contentStyle;
    /**
     * 内容行字体
     */
    private static Font contentFont;

    /**
     * 初始化HSSFWorkbook
     *
     * @return HSSFWorkbook
     * @Method_Name : init
     */
    private static HSSFWorkbook init() {
        HSSFWorkbook wb = new HSSFWorkbook();

//        headStyle = wb.createCellStyle();
//        headFont = wb.createFont();
        contentStyle = wb.createCellStyle();
        contentFont = wb.createFont();
        //内容行样式
        initContentCellStyle();
        //内容行字体
        initContentFont();

        return wb;
    }

    /**
     * 根据数据集合生成excel文件并下载
     *
     * @throws Exception
     * @Method_Name : downloadExcel
     */
    public static void downloadExcel(ExportFileDTO exportFileDTO) throws Exception {
        HSSFWorkbook wb = init();
        HSSFSheet sheet = wb.createSheet(exportFileDTO.getSheetName());
       for(int i = 0;i<=exportFileDTO.getNumCol();i++){
           if (i==0){
               sheet.setColumnWidth(i, 20 * 500);
           }else {
               if (exportFileDTO.getNumCol() < 8){
                   sheet.setColumnWidth(i, 20 * 200);
               }
           }
       }
        sheet.setDefaultColumnWidth(exportFileDTO.getNumCol());
        List<String> colContents = new ArrayList<>();
        Map<Integer, String> colMapContents = new HashMap<>();
        LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map = exportFileDTO.getMap();
        if (map.size() != 0) {
            for (Integer row : map.keySet()) {
                if (map.get(row).size() != 0) {
                    LinkedHashMap<Integer, ? extends ExcelDTO> colMap = map.get(row);
                    for (Integer col : colMap.keySet()) {
                        if (StringUtils.isNotEmpty(colMap.get(col)) && StringUtils.isNotEmpty(colMap.get(col).getFiled())) {
                            colMapContents.put(col, colMap.get(col).getFiled());
                        }
                    }
                }
            }
        }
        colMapContents.forEach((k, v) -> colContents.add(v));

        List<ExcelMergedDTO> excelMergedDTOS = exportFileDTO.getExcelMergedDTOS();
        if (CollectionUtils.isNotEmpty(excelMergedDTOS)) {
            for (ExcelMergedDTO excelMergedDTO : excelMergedDTOS) {
                sheet.addMergedRegion(
                        new CellRangeAddress(excelMergedDTO.getMergedBeginRow()
                                , excelMergedDTO.getMergedEndRow(), excelMergedDTO.getMergedBeginCol(), excelMergedDTO.getMergedEndCol()));
            }
        }


        //表头
        creatTableHeadRow(sheet, exportFileDTO, wb);

        if (CollectionUtils.isNotEmpty(exportFileDTO.getList())) {
            creatTableDataRows(sheet, exportFileDTO, colContents);
        }

        adjustColumnSize(sheet, exportFileDTO.getNumCol());
        String fileName = new SimpleDateFormat("yyyyMMdd").format(new Date()) + "_" + exportFileDTO.getFileName() + ".xls";
        outWrite(exportFileDTO.getRequest(), exportFileDTO.getResponse(), wb, fileName);
    }

    private static void outWrite(HttpServletRequest request, HttpServletResponse response, HSSFWorkbook wb,
                                 String fileName) throws IOException {
        OutputStream output = null;
        try {
            String userAgent = request.getHeader("User-Agent");
            output = response.getOutputStream();
            response.reset();
            response.setContentType("application/x-download charset=UTF-8");
            String filenamedisplay = URLEncoder.encode(fileName, "UTF-8");
            if ("FF".equals(getBrowser(request))) {
                filenamedisplay = new String(fileName.getBytes("UTF-8"),
                        "iso-8859-1");
            }
            response.setHeader("Content-Disposition", "attachment; filename=" + filenamedisplay);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

//            response.setHeader("Content-Length",fileSize.ToString());
            wb.write(output);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR ,e);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static String getBrowser(HttpServletRequest request) {
        String UserAgent = request.getHeader("USER-AGENT").toLowerCase();
        if (UserAgent.indexOf("msie") >= 0) {
            return "IE";
        }

        if (UserAgent.indexOf("firefox") >= 0) {
            return "FF";
        }

        if (UserAgent.indexOf("safari") >= 0) {
            return "SF";
        }

        return null;
    }

    /**
     * 创建表头行(需合并单元格)
     */
    private static void creatTableHeadRow(HSSFSheet sheet, ExportFileDTO exportFileDTO, HSSFWorkbook wb) {
        LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map = exportFileDTO.getMap();
        Map<Integer, Short> rowHeight = exportFileDTO.getRowHeight();
        if (map.size() != 0) {
            for (Integer row : map.keySet()) {
                HSSFRow hssfRow = sheet.createRow(row);
                hssfRow.setHeight(null == rowHeight.get(row) ? 350 : rowHeight.get(row));
                creatTableHeadRow(hssfRow, map.get(row), sheet, wb);
            }
        }
    }

    private static void creatTableHeadRow(HSSFRow row, LinkedHashMap<Integer, ? extends ExcelDTO> map, HSSFSheet sheet, HSSFWorkbook wb) {
        // 列头名称
        HSSFCell cell = null;
        for (Integer cellNum : map.keySet()) {
            headStyle = wb.createCellStyle();
            headFont = wb.createFont();
            headFont.setFontName("宋体");
            headFont.setCharSet(Font.DEFAULT_CHARSET);
            headFont.setColor(IndexedColors.BLACK.getIndex());
            if (map.size() != 1) {
                //中间
                headFont.setBold(true);
                headStyle.setAlignment(HorizontalAlignment.CENTER_SELECTION);
                headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headStyle.setWrapText(true);
            }
            cell = row.createCell(cellNum);
            headFont.setFontHeightInPoints(map.get(cellNum).getFontHeightInPoints() == null ? 10 : map.get(cellNum).getFontHeightInPoints());
            if (StringUtils.isNotEmpty(map.get(cellNum).getFontColor())) {
                headFont.setColor(map.get(cellNum).getFontColor());
            }
            headStyle.setFont(headFont);
            if (StringUtils.isNotEmpty(map.get(cellNum).getBackgroundColor())) {
                headStyle.setFillForegroundColor(map.get(cellNum).getBackgroundColor());
            }
            if (map.size() != 1) {
                cell.setCellStyle(headStyle);
                cell.setCellValue(map.get(cellNum).getName());
            } else {
                headStyle.setWrapText(true);
                cell.setCellStyle(headStyle);
                cell.setCellValue(new HSSFRichTextString(map.get(cellNum).getName()));
            }
            if (StringUtils.isNotEmpty(map.get(cellNum).getComment())) {
                HSSFPatriarch p = sheet.createDrawingPatriarch();
                HSSFComment  comment = p.createComment(new HSSFClientAnchor(0, 0, 0, 0, (short)1, 2, (short)4, 4));
                //输入批注信息
                comment.setString(new HSSFRichTextString(map.get(cellNum).getComment()));
                //将批注添加到单元格对象中
                cell.setCellComment(comment);
            }

        }
    }


    /**
     * @Description: 创建表格数据
     */
    private static void creatTableDataRows(HSSFSheet sheet, ExportFileDTO exportFileDTO, List<String> colContents) {
        List<Map<String, Object>> list = exportFileDTO.getList();
        HSSFRow row = null;
        //除了表头行
        int num = exportFileDTO.getMap().size();
        for (int i = num; i < list.size() + num; i++) {
            row = sheet.createRow(i);
            row.setHeight((short) 350);
            createRowData(colContents, list.get(i - num), row);
        }
    }

    /**
     * 设置内容
     */
    private static void createRowData(List<String> colContents, Map<String, Object> maps, HSSFRow row) {
        HSSFCell cell = null;
        String colContent = "";
        for (int i = 0; i < colContents.size(); i++) {
            colContent = colContents.get(i);
            if (StringUtils.isNotEmpty(maps.get(colContent))) {
                cell = row.createCell(i);
                cell.setCellStyle(contentStyle);
                cell.setCellValue(maps.get(colContent).toString());
            }
        }
    }


    /**
     * : 自动调整列宽
     */
    private static void adjustColumnSize(HSSFSheet sheet, int colNum) {
        if (colNum < 8) {
            for (int i = 0; i < colNum + 1; i++) {
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + sheet.getColumnWidth(i) / 2);
            }
        }
//        }else{
//            for (int i = 1; i < colNum+1; i++) {
//                sheet.autoSizeColumn(i);
//            }
//        }
    }


    /**
     * : 初始化表头行样式
     */
    private static void initHeadCellStyle() {
//        //边框样式 及格式
//        headStyle.setBorderTop(BorderStyle.MEDIUM);
//        headStyle.setBorderBottom(BorderStyle.THIN);
//        headStyle.setBorderLeft(BorderStyle.THIN);
//        headStyle.setBorderRight(BorderStyle.THIN);
//        headStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
//        headStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
//        headStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
//        headStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }


    /**
     * 初始化内容行样式
     */
    private static void initContentCellStyle() {
        contentStyle.setAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentStyle.setFont(contentFont);
        //边框样式 及格式
//        contentStyle.setBorderTop(BorderStyle.THIN);
//        contentStyle.setBorderBottom(BorderStyle.THIN);
//        contentStyle.setBorderLeft(BorderStyle.THIN);
//        contentStyle.setBorderRight(BorderStyle.THIN);
//        contentStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
//        contentStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
//        contentStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
//        contentStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        // 字段换行
        contentStyle.setWrapText(true);
    }


    /**
     * 初始化内容行字体
     */
    private static void initContentFont() {
        contentFont.setFontName("宋体");
        contentFont.setFontHeightInPoints((short) 10);
        contentFont.setBold(false);
        contentFont.setCharSet(Font.DEFAULT_CHARSET);
        contentFont.setColor(IndexedColors.BLACK.getIndex());
    }
}