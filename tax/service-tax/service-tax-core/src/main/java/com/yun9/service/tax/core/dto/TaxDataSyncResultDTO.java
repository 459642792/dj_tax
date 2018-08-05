package com.yun9.service.tax.core.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaxDataSyncResultDTO implements Serializable {
    private String type;
    private boolean success;
    private String message;
}
