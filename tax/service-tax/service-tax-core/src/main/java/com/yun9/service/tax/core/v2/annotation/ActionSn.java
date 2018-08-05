package com.yun9.service.tax.core.v2.annotation;

/**
 * Created by werewolf on  2018/5/7.
 */
public enum ActionSn {
    get_taxes(ActionTarget.task2), //下载所有税种
    get_tax(ActionTarget.task), //下载单税种
    syn_screenshot(ActionTarget.task2),//同步截图

    notice_inst(ActionTarget.task2), // 通知机构申报状态
    //税种 作废
    repeal_tax(ActionTarget.task2),

    send_bit_y(ActionTarget.task),//发送企业所得税报表
    send_deduct(ActionTarget.task2),//发起扣款

    get_fr_y(ActionTarget.task), //下载年度财务报表
    get_vat_q(ActionTarget.task2),//下载增值税
    get_personal_item_m(ActionTarget.task2),//下载个税人员清单

    send_fr_y(ActionTarget.task2),
    send_vat_q(ActionTarget.task2),
    send_bitb_q(ActionTarget.task2),
    send_bita_q(ActionTarget.task2),

    send_fr_q(ActionTarget.task2),

    send_check_vat_q(ActionTarget.task2),
    //发起个税工资薪金
    send_personal_payroll(ActionTarget.task2),
    //发起个税工资薪金 ,按上月
    send_personal_payroll_by_last_month(ActionTarget.task2),
    //发起 个体经营t
    send_personal_business(ActionTarget.task2),


    get_company_bank(ActionTarget.task2),

    //发送印花税
    send_yh_m(ActionTarget.task2),
    send_fz_m(ActionTarget.task2),
    send_fz_q(ActionTarget.task2),

    //同步客户资料
    syn_company_tax(ActionTarget.task3),

    //查询地税扣款记录接口
    find_deduct(ActionTarget.task2);
    private ActionTarget target;


    ActionSn(ActionTarget target) {
        this.target = target;
    }

    public ActionTarget getTarget() {
        return target;
    }
}
