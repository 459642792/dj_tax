package com.yun9.service.tax.core.task.callback.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.BizMdAreaService;
import com.yun9.biz.md.domain.entity.BizMdArea;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxInstanceService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.JsonUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalMFactory;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollItemFactory;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @Author: chenbin
 * @Date: 2018-06-05
 * @Time: 16:43
 * @Description:
 */
@TaskSnMapping(sns = {"SZ0003", "GD0003"})
public class GetPersonalItemCallback extends AbstractCallbackHandlerMapping {
    @Autowired
    BizTaxDeclareService bizTaxDeclareService;

    @Autowired
    BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    BizMdAreaService bizMdAreaService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    TaxInstanceCategoryPersonalMFactory taxInstanceCategoryPersonalMFactory;

    @Autowired
    TaxInstanceCategoryPersonalPayrollItemFactory taxInstanceCategoryPersonalPayrollItemFactory;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId())).orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.task_callback_failed, "该条个税工资薪金所得数据不存在"));

        //解析数据
        List<BizTaxInstanceCategoryPersonalPayrollItem> itemList = formatData(context.getBody(),context.getBizTaxInstance().getMdAreaId());

        if (CollectionUtils.isNotEmpty(itemList)) {
            //调用批量保存
            taxInstanceCategoryPersonalPayrollItemFactory.bacthCreate(context.getBizTaxInstance().getMdInstClientId(), context.getBizTaxInstance().getMdCompanyId(), context.getBizTaxInstance().getMdAccountCycleId(), context.getBizTaxInstance().getMdAreaId(), bizTaxInstanceCategoryPersonalPayroll.getId(), itemList);
        }

        bizTaxDeclareService.completeBeforeDeclare(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId(),
                context.getBizTaxInstanceSeq().getSeq(), context.getBody(), null);
        return context;
    }

    private List<BizTaxInstanceCategoryPersonalPayrollItem> formatData(String dataStr,long areaId) {
        if (StringUtils.isEmpty(dataStr)) {
            Collections.emptyList();
        }

        BizMdArea bizMdArea = Optional.ofNullable(bizMdAreaService.findById(areaId)).orElseThrow(()->BizTaxException.build(BizTaxException.Codes.task_callback_failed,"不支持的地区"));

        JSONArray itemJsonArr = null;

        JSONObject dataJsonObj = JsonUtils.parseObject(dataStr);
        if (bizMdArea.getSn().equals("guangdong")) {
            itemJsonArr = dataJsonObj.getJSONArray("items");
        }else if(bizMdArea.getSn().equals("shenzhen")){
            itemJsonArr = dataJsonObj.getJSONArray("data");
        }else{
            throw BizTaxException.build(BizTaxException.Codes.task_callback_failed,"不支持的地区");
        }

        List<BizTaxInstanceCategoryPersonalPayrollItem> itemList = new ArrayList<BizTaxInstanceCategoryPersonalPayrollItem>();
        if (itemJsonArr == null){
            return Collections.EMPTY_LIST;
        }
        for (int i = 0; i < itemJsonArr.size(); i++) {
            JSONObject itemJsonObj = itemJsonArr.getJSONObject(i);

            String itemJsonStr = JSON.toJSONString(itemJsonObj);
            BizTaxInstanceCategoryPersonalPayrollItem item = JsonUtils.parseObject(itemJsonStr, BizTaxInstanceCategoryPersonalPayrollItem.class);
            if (StringUtils.isEmpty(item.getCountryid())){
                item.setCountryid("156");
            }
            //item.setCountryname("");//无
            item.setAnnuity(new BigDecimal(itemJsonObj.getString("nianjin")));
            item.setDetailcode(itemJsonObj.getString("zspmGyDm"));
            //item.setDetailname("");//征税品目名称
            //item.setBegindate("");//工资所属期起
            //item.setEnddate("");//工资所属期止


            itemList.add(item);
        }
        return itemList;
    }
}
