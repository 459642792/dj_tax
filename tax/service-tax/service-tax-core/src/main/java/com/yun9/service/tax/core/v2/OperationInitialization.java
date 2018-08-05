package com.yun9.service.tax.core.v2;

import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import com.yun9.service.tax.core.v2.handler.TaxRouterBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by werewolf on  2018/5/7.
 */
public class OperationInitialization {

    private static final Map<String, TaskStartHandler> taskStartHandlerMap = new ConcurrentHashMap<>();
    private static final Map<String, TaskStartHandler2> taskStartHandlerMap2 = new ConcurrentHashMap<>();
    private static final Map<String, TaxRouterBuilder> taxRouterBuilderMap = new ConcurrentHashMap<>();

    @Autowired
    private List<TaskStartHandler> taskStartHandlers;

    @Autowired
    private List<TaskStartHandler2> taskStartHandlers2;

    @Autowired
    private List<TaxRouterBuilder> taxRouterBuilders;

    @PostConstruct
    public void init() {

        taskStartHandlers.forEach(v -> {
            ActionMapping actionsMapping = v.getClass().getAnnotation(ActionMapping.class);
            for (SnParameter parameter : actionsMapping.parameters()) {
                taskStartHandlerMap.put(this.buildKey(parameter), v);
            }
        });

        taskStartHandlers2.forEach(v -> {
            ActionMapping actionsMapping = v.getClass().getAnnotation(ActionMapping.class);
            for (SnParameter parameter : actionsMapping.parameters()) {
                taskStartHandlerMap2.put(this.buildKey(parameter), v);
            }
        });

        taxRouterBuilders.forEach(v -> {
            ActionMapping actionsMapping = v.getClass().getAnnotation(ActionMapping.class);
            for (SnParameter parameter : actionsMapping.parameters()) {
                taxRouterBuilderMap.put(this.buildKey(parameter), v);
            }
        });
    }

    private String buildKey(SnParameter snParameter) {
        StringJoiner stringJoiner = new StringJoiner("_");
        stringJoiner.add(snParameter.sn().name());
        if (null != snParameter.taxOffice()) {
            stringJoiner.add(snParameter.taxOffice().name());
        }
        if (null != snParameter.cycleType()) {
            stringJoiner.add(snParameter.cycleType().name());
        }
        if (null != snParameter.area()) {
            stringJoiner.add(snParameter.area().name());
        }
        return stringJoiner.toString();
    }

    private String buildKey(ActionSn sn, TaxOffice taxOffice, CycleType cycleType, String areaSn) {
        StringJoiner stringJoiner = new StringJoiner("_");
        if (null == sn) {
            throw new RuntimeException("actionSN参数必须配置");
        }
        stringJoiner.add(sn.name());
        if (null != taxOffice) {
            stringJoiner.add(taxOffice.name());
        }
        if (null != cycleType) {
            stringJoiner.add(cycleType.name());
        }
        if (StringUtils.isNotEmpty(areaSn)) {
            stringJoiner.add(areaSn);
        }
        return stringJoiner.toString();
    }

    protected TaskStartHandler taskStartHandler(ActionSn sn, TaxOffice taxOffice, CycleType cycleType, String areaSn) {
        return taskStartHandlerMap.get(this.buildKey(sn, taxOffice, cycleType, areaSn));
    }

    protected TaskStartHandler2 taskStartHandler2(ActionSn sn, TaxOffice taxOffice, CycleType cycleType, String areaSn) {
        return taskStartHandlerMap2.get(this.buildKey(sn, taxOffice, cycleType, areaSn));
    }

    protected TaxRouterBuilder taxRouterBuilder(ActionSn sn, TaxOffice taxOffice, CycleType cycleType, String areaSn) {
        return taxRouterBuilderMap.get(this.buildKey(sn, taxOffice, cycleType, areaSn));
    }

}
