package com.yun9.service.tax.core.enums;

import lombok.Getter;
import lombok.Setter;

public class TaxLabelEnum {
    public enum Lable {
        key, value, count;
    }


    /**
     * 缴税账户状态
     */
    public enum CompanyBankStateEnum {
        /**
         * 无缴税账户
         */
        NO_TAX_ACCOUNT("companyBank", "noCompanyBank"),
        /**
         * 未确认账户
         */
        UN_TAX_ACCOUNT("companyBank", "unCompanyBank"),
        /**
         * 已确认账户
         */
        TAX_ACCOUNT("companyBank", "companyBank");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        CompanyBankStateEnum(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 税额状态
     */
    public enum TaxDeclareType {
        /**
         * 无税申报
         */
        TAX_FREE("taxDeclareType", "taxfree"),
        /**
         * 有税申报
         */
        TAX_ABLE("taxDeclareType", "taxable");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        TaxDeclareType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }
    /**
     * 税额状态
     */
    public enum VatTaxDeclareType {
        /**
         * 零申报
         */
        ZERO("taxDeclareType", "zero"),
        /**
         * 无税申报
         */
        TAX_FREE("taxDeclareType", "taxfree"),
        /**
         * 有税申报
         */
        TAX_ABLE("taxDeclareType", "taxable"),
        /**
         * 退税申报
         */
        DRAWBACK("taxDeclareType", "drawback");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        VatTaxDeclareType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 报表状态
     */
    public enum ReportAudit {
        /**
         * 报表未确认
         */
        UN_AUDIT("reportAuditType", "reportUnAudit"),
        /**
         * 报表已确认
         */
        AUDIT("reportAuditType", "reportAudit");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        ReportAudit(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 审核状态
     */
    public enum TaxAudit {
        /**
         * 未审核
         */
        UN_AUDIT("taxAuditType", "taxUnAudit"),
        /**
         * 已审核
         */
        AUDIT("taxAuditType", "taxAudit");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        TaxAudit(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }


    /**
     * 申报方式
     */
    public enum DeclareType {
        /**
         * 无需申报
         */
        UN_DECLARE("declareType", "undeclare"),
        /**
         * 手动申报
         */
        HAND_WORK("declareType", "handwork"),
        /**
         * 申报成功
         */
        YUN_9("declareType", "yun9"),
        /**
         * 网上申报
         */
        TAX_OFFICE("declareType", "taxOffice");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        DeclareType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 备案制度
     */
    public enum TaxRecord {
        /**
         * 未备案
         */
        NONE("taxRecord", "none"),
        /**
         * 已备案
         */
        RECORD("taxRecord", "records");

        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        TaxRecord(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 数据来源
     */
    public enum SourceType {
        /**
         * 按上月
         */
        LAST_MONTH("sourceType", "last"),
        /**
         * 按实际
         */
        PRACTICAL("sourceType", "hand"),
        /**
         * 首次申报
         */
        FIRST("sourceType", "first");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        SourceType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 增值税状态->印花税
     */
    public enum FhVatStateType {
        /**
         * 未审核
         */
        UN_AUDIT("fhVatStateType", "unaudit"),
        /**
         * 已审核
         */
        AUDIT("fhVatStateType", "audit"),
        /**
         * 已申报
         */
        SUCCESS("fhVatStateType", "success");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        FhVatStateType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     * 增值税状态->附征税
     */
    public enum FzDirectTaxStateType {
        /**
         * 增值税销售额不大于10万
         */
        VAT_SALE_AMOUNT("fzDirectTaxStateType", "vatSaleAmount"),
        /**
         * 增值税销售额不大于30万
         */
        VAT_SALE_AMOUNT_THREE("fzDirectTaxStateType", "vatSaleAmountThree"),
        /**
         * 增值税零申报
         */
        VAT_ZERO("fzDirectTaxStateType", "vatZero"),
        /**
         * 主税未完成审核
         */
        DIRECT_TAX_UN_AUDIT("fzDirectTaxStateType", "directTaxUnAudit"),
        /**
         * 主税已审核
         */
        DIRECT_TAX_AUDIT("fzDirectTaxStateType", "directTaxAudit"),
        /**
         * 主税已申报
         */
        DIRECT_TAX_SUCCESS("fzDirectTaxStateType", "directTaxSuccess");
        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        FzDirectTaxStateType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }

    /**
     *
     */
    public enum InstClientStateType {
        /**
         * 停止服务
         */
        VAT_SALE_AMOUNT("instClientStateType", "disabled");

        @Getter
        @Setter
        private String defSn;
        @Getter
        @Setter
        private String sn;

        InstClientStateType(String defSn, String sn) {
            this.defSn = defSn;
            this.sn = sn;
        }
    }


}
