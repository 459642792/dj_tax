package com.yun9.service.tax.core.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class ExcelMergedDTO {
    Integer mergedBeginRow;
    Integer mergedEndRow;
    Integer mergedBeginCol;
    Integer mergedEndCol;
}
