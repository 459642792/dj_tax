package com.yun9.service.tax.controller;


import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxDataSynchronizationFactory;
import com.yun9.service.tax.core.dto.TaxDataNoticeInstDTO;
import com.yun9.service.tax.core.dto.TaxDataSyncDTO;
import com.yun9.service.tax.core.dto.TaxDataUnAuditDTO;
import com.yun9.service.tax.core.dto.TaxDataUpdateClientDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 税务数据，同步控制器
 * 用于第三方同步数据给智财税
 */
@Controller
@RequestMapping("/sync")
public class TaxDataSynchronizationController {
    @Autowired
    private TaxDataSynchronizationFactory taxDataSynchronizationFactory;

    @RequestMapping(value = "/instupdatedata", method = RequestMethod.POST)
    @ResponseBody
    public Object allTax(@RequestBody TaxDataSyncDTO taxDataSyncDTO,
                         @User UserDetail userDetail) {
        return taxDataSynchronizationFactory.instUpdateData(taxDataSyncDTO,userDetail.getId());
    }

    @RequestMapping(value = "/instresetuploadstate", method = RequestMethod.POST)
    @ResponseBody
    public Object instResetUploadState(@RequestBody TaxDataUnAuditDTO taxDataUnAuditDTO,
                         @User UserDetail userDetail) {
        return taxDataSynchronizationFactory.instResetUploadState(taxDataUnAuditDTO,userDetail.getId());
    }

    @RequestMapping(value = "/instupdateclients", method = RequestMethod.POST)
    @ResponseBody
    public Object updateClients(@RequestBody TaxDataUpdateClientDTO taxDataUpdateClientDTO,
                         @User UserDetail userDetail) {
        return taxDataSynchronizationFactory.instUpdateClients(taxDataUpdateClientDTO,userDetail.getId());
    }

    @RequestMapping(value = "/noticeinst", method = RequestMethod.POST)
    @ResponseBody
    public Object noticeInst(@RequestBody TaxDataNoticeInstDTO taxDataNoticeInstDTO,
                                @User UserDetail userDetail) {
        return taxDataSynchronizationFactory.noticeInst(taxDataNoticeInstDTO,userDetail.getId());
    }

}
