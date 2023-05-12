package me.ltang.rules.model;

import com.alibaba.fastjson.JSONObject;

/**
 * 请求报文
 *
 * @author tangliu
 */
public class RuleRequest {

    /**
     * 业务类型：
     * 1001： 信贷规则
     * 1002： 风险规则
     * 1003： 营销规则
     * 1004： 信审规则
     * ...
     */
    private String businessType;

    /**
     * 规则名称
     */
    private String ruleId;

    /**
     * 入参
     */
    private JSONObject params;


    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public JSONObject getParams() {
        return params;
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }
}
