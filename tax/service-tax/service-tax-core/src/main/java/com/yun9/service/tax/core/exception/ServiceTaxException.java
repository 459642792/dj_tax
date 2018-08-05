package com.yun9.service.tax.core.exception;

import com.yun9.commons.exception.BizException;

/**
 * 税务服务异常
 */
public class ServiceTaxException extends BizException {
    private static final int mainCode = 700000;

    public ServiceTaxException(String message) {
        super(message);
    }

    public ServiceTaxException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ServiceTaxException(String message, Integer code) {
        super(message);
        this.setCode(code + mainCode);
    }

    public ServiceTaxException(String message, Integer code, Throwable throwable) {
        super(message, throwable);
        this.setCode(code + mainCode);
    }

    public static ServiceTaxException build(Codes codes, Throwable throwable, Object... params) {
        return new ServiceTaxException(String.format(codes.getMessage(), params), codes.getCode(), throwable);
    }

    public static ServiceTaxException build(Codes codes, Object... params) {
        return new ServiceTaxException(String.format(codes.getMessage(), params), codes.getCode());
    }


    public enum Codes {
        TaskCallBackResponseNotNull(1001, "任务回调请求不能为空"),
        TaskCallSeqNotNull(1002, "任务Seq不能为空!"),
        TaskCallTaskSnNotNull(1003, "任务回调错误，无法找到task-sn"),
        TaskCallBackHandlerNotFound(1004, "任务回调处理器未找到."),
        TaskStartBuildHandlerNotFound(1005, "任务构建处理器未找到"),
        TaskStartActionHandlerNotFound(1006, "任务发起处理器未找到"),
        TaxInstanceCreateFailed(1007, "申报实例创建失败"),
        TaskStartFailed(1008, "任务发起失败 %s"),
        TaskCallBackResolveDateFailed(1009, "在处理回调任务%s时解析body数据 %s 出错"),
        //        TaxCategorysBodyIsEmpty(1010,"下载税种数据为空"),
        ReportSnNotFound(1010, "获取报表sn失败，系统没有配置"),
        ReportCreateException(1010, "创建报表失败:%s"),

        TaxConfigInfoNotFound(1011, "税种配置信息未找到"),
        BizTypeNotFound(1012, "业务操作%s 未找到"),
        BuildParametersError(1013, "构建参数 %s 错误。"),
        CallbackProcessFailed(1014, "回调处理失败:%s"),
        NotSupportTask(1015, "不支持的业务操作%s"),
        TaskCallFailed(1016, "任务发起失败"),
        IllegalArgumentException(1017, "参数错误 %s "),

        //===============
        company_has_not_default_account(1018, "没有设置账号"),
        task_start_failed_has_not_task_sn(1009, "发起任务失败,没有找到系统配置任务sn"),
        task_start_send_failed(1010, "发送任务失败"),
        task_start_send_failed1(1010, "发送任务失败%s"),
        tax_state_error(1011, "当前执行操作 %s 状态为 %s  不能执行下一步请求"),
        report_data_not_found(1012, "报表数据不存在"),
        TASK_NOT_ALLOWED_SEND(1013, "任务不允许发生  原因 %s"),
        tax_router_config_not_found(1014, "税务路由配置信息 %s 未找到"),

        TAX_VAT_CYCLE_ERRORS(1015, "申报时间格式错误:%s"),
        md_code_not_found(1016, "无法找到控制编码"),


        EXPORT_EXCEL_ERROR(1016, "下载模板错误:%s"),
        BILL_AUDIT_ERROR(1017, "审核错误:%s"),
        AUDIT_ERROR(1017, "审核错误:%s"),
        do_not_need_deduct(1018, "税额已缴清或不满足扣款条件"),
        deduct_state_error(1019, "发起扣款截图状态错误"),
        declare_state_error(1020, "申报状态错误: %s"),
        declare_info_not_found(1021, "申报信息为找到 ：%s"),
        not_found_report_generate_handler(2000, "没有找到报表%s生成器"),
        task_callback_success_report_generate(2001, "回调处理成功,报表生成失败:%s"),
        task_start_faied(2002, "任务发起失败 : %s"),
        report_current_data_not_found(2003, "无法找到税种当前数据"),
        report_history_data_not_found(2004, "无法找到税种当前数据"),
        report_clientinfo_data_not_found(2005, "无法找到税局客户资料"),
        send_deduct_state_error(2006, "发起扣款状态错误：%s "),
        send_tax_state_error(2006, "发起申报状态错误：%s "),
        repeal_tax_state_error(2006, "发起作废状态错误：%s "),
        prepaid_state_error(2007, "预缴状态错误"),
        agent_error(2008, "代开发票数据有误"),
        vat_tax_not_found(2009, "增值税还未申报"),
        personal_tax_not_found(2010, "个税还未申报"),
        personal_not_found(2011, "获取个税人员信息失败");



        private final Integer code;
        private String message;

        Codes(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return this.code;
        }

        public String getMessage() {
            return this.message;
        }

    }

    public static void throwException(Codes codes, Object... objects) {
        throw ServiceTaxException.build(codes, objects);
    }
}
