package com.yun9.service.tax.core.event;

import lombok.Data;

@Data
public class NoticeInstEvent extends ServiceTaxEvent {
    private  long bizTaxInstanceCategoryId;
}
