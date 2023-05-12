package me.ltang.rules.util;

import com.alibaba.fastjson.JSONObject;

/**
 * 封装返回结果
 *
 * @author tangliu
 */
public class ResponseUtil {

    public static JSONObject succeed(JSONObject rst, Object data) {
        rst.put("code", "0000");
        rst.put("msg", "请求成功");
        rst.put("attach", "");
        rst.put("data", data);
        return rst;
    }

    public static JSONObject succeed(Object data) {
        JSONObject rst = new JSONObject();
        return succeed(rst, data);
    }

    public static JSONObject failed(RuleException exception) {
        JSONObject rst = new JSONObject();
        rst.put("code", exception.getCode());
        rst.put("msg", "规则执行异常!");
        rst.put("attach", exception.getMsg());
        rst.put("data", "");
        return rst;
    }

    public static JSONObject failed(String message) {
        JSONObject rst = new JSONObject();
        rst.put("code", "500");
        rst.put("msg", message);
        return rst;
    }
}
