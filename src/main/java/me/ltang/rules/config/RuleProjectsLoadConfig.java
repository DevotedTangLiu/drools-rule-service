package me.ltang.rules.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化加载所有规则project配置
 *
 * @author tangliu
 */
@Configuration
public class RuleProjectsLoadConfig {

    private static Logger logger = LoggerFactory.getLogger(RuleProjectsLoadConfig.class);

    /**
     * workbench的登陆用户名（需要有下载jar包的权限的用户）
     */
    @Value("${drools.workbench.username}")
    private String username;

    /**
     * workbench的登陆密码
     */
    @Value("${drools.workbench.password}")
    private String password;

    @Bean(initMethod = "init")
    public RuleHolder ruleHolder() {
        return new AsyncRuleHolder(username, password);
    }
}
