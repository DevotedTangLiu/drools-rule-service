package me.ltang.rules.config;

import org.drools.compiler.kproject.ReleaseIdImpl;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 对应一个project的规则资源
 *
 * @author tangliu
 */
public class ProjectResource {

    private static Logger logger = LoggerFactory.getLogger(ProjectResource.class);

    private KieContainer kieContainer;

    private KieBase kieBase;

    private KieScanner kieScanner;

    private final Map<String, RuleMethodProperties> methods = new HashMap<>();

    private static final KieServices ks = KieServices.Factory.get();

    public void init(String groupId, String artifactId, String version) {

        logger.info("开始初始化项目...");
        logger.info("groupId:   " + groupId);
        logger.info("artifactId:" + artifactId);
        logger.info("version:   " + version);
        /**
         * 2021-07-24
         */
        ReleaseIdImpl releaseId = new ReleaseIdImpl(groupId, artifactId, version);
        kieContainer = ks.newKieContainer(releaseId);

        kieBase = kieContainer.getKieBase();
    }

    /**
     * 立即扫描规则更新
     */
    public void scanNow() {
        kieScanner.scanNow();
    }

    /**
     * 停止更新规则
     */
    public void scanStop() {
        if (kieScanner != null) {
            kieScanner.stop();
            kieScanner.shutdown();
            kieScanner = null;
        }
    }

    /**
     * 启动扫描更新任务
     */
    public void scanStart() {
        if (kieScanner != null) {
            kieScanner.stop();
            kieScanner.shutdown();
            kieScanner = null;
        }
        /**
         * 每30s 更新一次规则jar，保证jar为最新
         */
        kieScanner = ks.newKieScanner(kieContainer);
        kieScanner.start(30000L);
    }

    /**
     * 销毁
     */
    public void destroy() {
        if (kieScanner != null) {
            kieScanner.shutdown();
            kieScanner = null;
        }
        if (kieContainer != null) {
            kieContainer.dispose();
        }
    }

    /**
     * 添加方法信息
     *
     * @param methodId
     * @param methodProperties
     */
    public void addMethod(String methodId, RuleMethodProperties methodProperties) {
        this.methods.put(methodId, methodProperties);
    }

    /**
     * 获取方法信息
     *
     * @param methodId
     * @return
     */
    public RuleMethodProperties getMethod(String methodId) {
        return this.methods.get(methodId);
    }

    public KieBase getKieBase() {
        return kieBase;
    }


    /**
     * project中的方法关联的入参和出参等信息
     */
    public static class RuleMethodProperties {

        private String ruleId;
        private String ruleName;
        private String ruleNameType;
        private String ruleType;
        private String request;
        private String response;
        private boolean insertResult;

        public RuleMethodProperties(String ruleId, String ruleName, String ruleNameType, String ruleType, String request, String response, boolean insertResult) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.ruleNameType = ruleNameType;
            this.ruleType = ruleType;
            this.request = request;
            this.response = response;
            this.insertResult = insertResult;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getRuleNameType() {
            return ruleNameType;
        }

        public void setRuleNameType(String ruleNameType) {
            this.ruleNameType = ruleNameType;
        }

        public String getRuleType() {
            return ruleType;
        }

        public void setRuleType(String ruleType) {
            this.ruleType = ruleType;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public boolean isInsertResult() {
            return insertResult;
        }

        public void setInsertResult(boolean insertResult) {
            this.insertResult = insertResult;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }
    }
}
