package com.yun9.service.tax.controller;

import com.yun9.biz.md.BizMdInstOrgTreeUserService;
import com.yun9.biz.md.BizMdInstUserService;
import com.yun9.biz.md.domain.entity.BizMdInstOrgTreeUser;
import com.yun9.biz.md.domain.entity.BizMdInstUser;
import com.yun9.biz.tax.BizTaxInstanceCategoryHistoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFzItem;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryHistory;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryFzFactory;
import com.yun9.service.tax.core.dto.BizTaxFzItemDTO;
import com.yun9.sys.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * Created by werewolf on  2018/5/10.
 */
@Controller
@RequestMapping("instance/category/history")
public class TaxInstanceCategoryHistoryController {


    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;

    @Autowired
    BizMdInstUserService bizMdInstUserService;

    @Autowired
    SysUserService sysUserService;

    @GetMapping("/list/{taxInstanceCategoryId}")
    @ResponseBody
    public Pagination<HashMap> list(@PathVariable long taxInstanceCategoryId,
                                    @QueryParam QueryJson query, @PageParam Pageable pageable) {

        Map<String, Object> params = new HashMap() {{
            put("type", query.getString("type").orElse(null));
            put("state", query.getString("state").orElse(null));
            put("id", query.getLong("id").orElse(null));//附加税id
        }};


        Pagination pagination = new Pagination();
        Pagination<BizTaxInstanceCategoryHistory> pageObj = bizTaxInstanceCategoryHistoryService.listByType(taxInstanceCategoryId, params, pageable.getPage(), pageable.getLimit());
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());


        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                    add(new HashMap() {{
                        put("taxInstanceCategoryId",v.getBizTaxInstanceCategoryId());//id
                        put("createBy",v.getCreateBy());//创建者id
                        put("remark",v.getRemark());//remark
                        put("type",v.getType());//类型
                        BizMdInstUser user =  bizMdInstUserService.findById(v.getCreateBy());
                        String name = "未知";
                        if (user != null){
                            name = Optional.ofNullable(sysUserService.findById(user.getUserId())).map(v->v.getName()).orElse(null);
                        }
                        put("name",name);
                        put("state",v.getState());
                        put("createdAt",v.getCreatedAt());
                    }});
                });
            }
        });
        return pagination;
    }

}
