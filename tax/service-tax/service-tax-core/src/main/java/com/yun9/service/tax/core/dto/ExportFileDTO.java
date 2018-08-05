package com.yun9.service.tax.core.dto;

import lombok.Data;
import lombok.NonNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExportFileDTO {
    @NonNull
    HttpServletRequest request;
    @NonNull
    HttpServletResponse response;
    /**
     * 内容行
     */
    List<Map<String, Object>> list;
    /**
     * 行高
     */
    @NonNull
    Map<Integer, Short> rowHeight;
    /***
     * 合并行
     */
    List<ExcelMergedDTO> excelMergedDTOS;
    /**
     * 表头参数
     */
    @NonNull
    LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map;
    /**
     * 多少列
     */
    @NonNull
    Integer numCol;
    @NonNull
    String sheetName;
    @NonNull
    String fileName;

    public ExportFileDTO(HttpServletRequest request, HttpServletResponse response, List<Map<String, Object>> list, Map<Integer, Short> rowHeight, List<ExcelMergedDTO> excelMergedDTOS, LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map, Integer numCol, String sheetName, String fileName) {
        this.request = request;
        this.response = response;
        this.list = list;
        this.rowHeight = rowHeight;
        this.excelMergedDTOS = excelMergedDTOS;
        this.map = map;
        this.numCol = numCol;
        this.sheetName = sheetName;
        this.fileName = fileName;
    }
}
