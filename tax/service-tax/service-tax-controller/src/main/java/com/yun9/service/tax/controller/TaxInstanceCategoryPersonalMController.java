package com.yun9.service.tax.controller;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalMFactory;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollItemFactory;
import com.yun9.service.tax.core.dto.BizTaxPersonalImportDTO;
import com.yun9.service.tax.core.dto.BizTaxPersonalImportSheetDTO;
import com.yun9.service.tax.core.dto.PersonalHistoryPayrollDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.*;

/**
 * 个税工资薪金(月报)
 *
 * @Author: chenbin
 * @Date: 2018-05-31
 * @Time: 16:01
 * @Description:
 */
@Controller
@RequestMapping("/instance/category/personal/m")
public class TaxInstanceCategoryPersonalMController {

    public static final Logger logger = LoggerFactory
        .getLogger(TaxInstanceCategoryPersonalMController.class);

    @Value("${file.upload.path}")
    private String path;
    @Autowired
    private TaxInstanceCategoryPersonalMFactory taxInstanceCategoryPersonalMFactory;
    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;
    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;
    @Autowired
    TaxInstanceCategoryPersonalPayrollItemFactory taxInstanceCategoryPersonalPayrollItemFactory;


    /**
     * 获取个税历史申报列表
     */
    @GetMapping("history/list/{instClientId}/{mdCompanyId}/{areaId}/{mdAccountCycleIds}")
    @ResponseBody
    public List getHistoryList(@PathVariable Long instClientId,@PathVariable long mdCompanyId, @PathVariable long areaId,
        @PathVariable long[] mdAccountCycleIds) {
        List<PersonalHistoryPayrollDTO> personalHistoryPayrollDtoList = taxInstanceCategoryPersonalMFactory
            .getHistoryList(instClientId,mdCompanyId, areaId, mdAccountCycleIds);
        return Optional.ofNullable(personalHistoryPayrollDtoList).orElse(Collections.EMPTY_LIST);
    }

    @GetMapping("history/item/{payrollId}")
    @ResponseBody
    public BizTaxPersonalImportDTO getHistoryItems(@User UserDetail userDetail, @PathVariable long payrollId) {
        return taxInstanceCategoryPersonalMFactory
                .getHistoryItems(payrollId);
    }

    @GetMapping("update/sourcetype/{instanceCategoryPersonalPayrollId}/{sourceType}")
    @ResponseBody
    public boolean updateSourceType(@User UserDetail userDetail,
        @PathVariable long instanceCategoryPersonalPayrollId,
                                    @PathVariable BizTaxInstanceCategoryPersonalPayroll.SourceType sourceType) {
        return taxInstanceCategoryPersonalMFactory
            .updateSourceType(instanceCategoryPersonalPayrollId, sourceType, userDetail.getId());
    }

    /**
     * 导入数据
     */
    @RequestMapping(value = "/upload/{categoryId}", method = RequestMethod.POST)
    @ResponseBody
    public Object upload(@PathVariable long categoryId, @User UserDetail userDetail,
        MultipartHttpServletRequest request) {
        Iterator<String> itr = request.getFileNames();
        MultipartFile mpf = null;
        BizTaxPersonalImportDTO bizTaxPersonalImportDTO = null;

        while (itr.hasNext()) {
            mpf = request.getFile(itr.next());
        }
        try {
            request.setCharacterEncoding("UTF-8");
            logger.info("文件名称{}", mpf.getOriginalFilename());
            BizTaxPersonalImportSheetDTO importSheetDTO = new BizTaxPersonalImportSheetDTO();
            importSheetDTO.setFileData(mpf.getBytes());
            importSheetDTO.setFileOriginalName(mpf.getOriginalFilename());
            importSheetDTO.setFileUploadPath(path);
            importSheetDTO.setUserId(userDetail.getId());
            importSheetDTO.setCategoryId(categoryId);
            bizTaxPersonalImportDTO = taxInstanceCategoryPersonalMFactory
                .parsePersonalExcel(importSheetDTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null == bizTaxPersonalImportDTO) {
            throw BizTaxException.build(BizTaxException.Codes.DateError, "解析Excel失败!");
        }
        return JSON.toJSON(bizTaxPersonalImportDTO);
    }

    /**
     * 个税撤销确认审核
     */
    @RequestMapping(value = "/verify/unconfirmed/{bizTaxInstanceCategoryPayrollId}", method = RequestMethod.PUT)
    @ResponseBody
    public void updateAuditStateUnconfirmed(@PathVariable long bizTaxInstanceCategoryPayrollId,
                                            @User UserDetail userDetail) {
        taxInstanceCategoryPersonalMFactory
            .cancleAudit(bizTaxInstanceCategoryPayrollId, userDetail.getInstUserId(), "unconfirmed");
    }

    /**
     * 个税确认审核 (按上月确认会计区间)
     */
    @RequestMapping(value = "/verify/confirmed/{bizTaxInstanceCategoryPayrollId}", method = RequestMethod.PUT)
    @ResponseBody
    public void updateAuditStateConfirmed(@PathVariable long bizTaxInstanceCategoryPayrollId,
                                          @User UserDetail userDetail) {
        taxInstanceCategoryPersonalMFactory
            .audit(bizTaxInstanceCategoryPayrollId, userDetail.getInstUserId(),
                "confirmed");
    }

    /**
     * 添加个税人员
     *
     * @param bizTaxInstanceCategoryPersonalPayrollItem
     * @return
     */
    @RequestMapping(value = "/personalPayrollItem", method = RequestMethod.POST)
    @ResponseBody
    public Object addPersonalPayrollItem(@RequestBody BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem) {

        return taxInstanceCategoryPersonalMFactory.addPersonalPayrollItem(new ArrayList<BizTaxInstanceCategoryPersonalPayrollItem>(){{
            add(bizTaxInstanceCategoryPersonalPayrollItem);
        }},bizTaxInstanceCategoryPersonalPayrollItem.getBizTaxInstanceCategoryPersonalPayrollId(),false);

    }

    /**
     * 修改个税人员信息
     *
     * @param bizTaxInstanceCategoryPersonalPayrollItem
     * @return
     */
    @RequestMapping(value = "/personalPayrollItem", method = RequestMethod.PUT)
    @ResponseBody
    public Object updatePersonalPayrollItem(@RequestBody BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem) {

        return taxInstanceCategoryPersonalMFactory.updatePersonalPayrollItem(bizTaxInstanceCategoryPersonalPayrollItem);

    }


    /**
     * 批量删除
     *
     * @param itemIds
     */
    @RequestMapping(value = "/personalPayrollItem/{itemIds}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deletePersonalPayrollItem(@PathVariable Long[] itemIds) {

        List<Long> ids = Arrays.asList(itemIds);

        if (CollectionUtils.isEmpty(ids)) {

            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误，没有删除对象ID");
        }

        return taxInstanceCategoryPersonalMFactory.deletePersonalPayrollItem(ids);



    }

    @RequestMapping(value = "/personalPayrollItem", method = RequestMethod.GET)
    @ResponseBody
    public Object getPersonalPayrollItem(@QueryParam QueryJson query, @PageParam Pageable pageable) {



        Map<String, Object> params = query.getAll();

        if (null == params || params.size() == 0 || null == params.get("personalPayrollId")) {

            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误,传递参数为空");
        }

        String personalPayrollId = params.get("personalPayrollId").toString();

        if(StringUtils.isEmpty(personalPayrollId)) {

            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误,传递参数为空");
        }


        Map<String, Object> params_1 = new HashMap() {{
            put("personalPayrollId", personalPayrollId);
            put("useType", params.get("useType").toString());
        }};

        return taxInstanceCategoryPersonalMFactory.getPersonalPayrollItem(pageable.getPage(), pageable.getLimit(), params_1);

    }

    @RequestMapping(value = "/confirm/PersonalInfo/{categoryId}", method = RequestMethod.GET)
    @ResponseBody
    public Object confirmPersonalInfo(@PathVariable long categoryId) {

        return taxInstanceCategoryPersonalMFactory.confirmPersonalInfo(categoryId);

    }
    /**
     * 检查是否可以从税局下载历史人员清单
     *
     * @param categoryIds
     * @return
     */
    @PostMapping("down/item/check/{categoryIds}")
    @ResponseBody
    public List downItemFromTaxCheck(@PathVariable long[] categoryIds) {
        List<Map> backMapList = new LinkedList<Map>();
        Map<Integer, String> errMap = new HashMap<Integer, String>() {{
            put(-1, "税种不存在");
            put(1, "税种状态不为已发起");
            put(2, "处于办理中,不可下载");
        }};
        for (int i = 0; i < categoryIds.length; i++) {
            Map<String, Object> checkBackMap = taxInstanceCategoryPersonalMFactory.downItemFromTaxCheck(categoryIds[i]);
            int checkBackCode = Integer.parseInt(checkBackMap.get("code") + "");
            int finalI = i;
            Map<String, Object> backMap = new HashMap<String, Object>() {{
                put("categoryId", categoryIds[finalI]);
                int code = 0;
                if (checkBackCode != 0) {
                    if (checkBackCode == 4) {
                        code = 2;
                    } else {
                        code = 1;
                    }
                }
                put("code", code);
                put("msg", checkBackMap.get("msg"));
            }};
            backMapList.add(backMap);
        }

        return backMapList;
    }
}
