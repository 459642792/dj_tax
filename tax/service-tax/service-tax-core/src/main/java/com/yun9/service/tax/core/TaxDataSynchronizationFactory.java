package com.yun9.service.tax.core;

import com.yun9.service.tax.core.dto.*;

import java.util.List;

/**
 * 税务同步工程
 */
public interface TaxDataSynchronizationFactory {

    List<TaxDataSyncResultDTO> instUpdateData(TaxDataSyncDTO taxDataSyncDTO,long userId);

    Object instResetUploadState(TaxDataUnAuditDTO taxDataUnAuditDTO, long userId);


    Object instUpdateClients(TaxDataUpdateClientDTO taxDataUnAuditDTO, long userId);


    /*=============申报通知机构*/
    Object noticeInst(TaxDataNoticeInstDTO taxDataNoticeInstDTO, long userId);

}
