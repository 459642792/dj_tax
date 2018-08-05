package com.yun9.service.tax.core.dto;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFzItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-11 16:09
 */
@Data
public class BizTaxFzItemDTO implements Serializable{
   private long fzId; 
   private List<BizTaxInstanceCategoryFzItem> fzItems;
}
