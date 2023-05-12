package me.ltang.rules.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author tangliu
 */
@Configuration
public class Constants {

    /**
     * 普通规则
     */
    public static final String RULE_TYPE_NORMAL = "NORMAL";

    /**
     * 规则流
     */
    public static final String RULE_TYPE_FLOW = "FLOW";

    /**
     * 规则名 - 全匹配
     */
    public static final String RULE_NAME_PATTERN_EQUAL = "EQUAL";

    /**
     * 规则名 - 正则
     */
    public static final String RULE_NAME_PATTERN_MATCH = "MATCH";

    /**
     * 规则名 - 前缀匹配
     */
    public static final String RULE_NAME_PATTERN_START = "PRE";

    /**
     * 规则名 - 后缀匹配
     */
    public static final String RULE_NAME_PATTERN_END = "END";

    /**
     * 默认不插入结果对象
     */
    public static final boolean RULE_DEFALT_INSERT_RESULT = false;

    @Value("${default.request.clazz}")
    public void setDefaultInsertRequestClazz(String clazz) {
        DEFAULT_INSERT_REQUEST_CLAZZ = clazz;
    }

    @Value("${default.response.clazz}")
    public void setDefaultResponseClazz(String clazz) {
        DEFAULT_RESPONSE_CLAZZ = clazz;
    }

    public static String DEFAULT_INSERT_REQUEST_CLAZZ;

    public static String DEFAULT_RESPONSE_CLAZZ;
}
