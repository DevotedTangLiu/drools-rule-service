package me.ltang.rules.config;

import cn.hutool.setting.Setting;
import com.alibaba.fastjson.JSONObject;
import me.ltang.rules.util.Constants;
import me.ltang.rules.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 维护project规则
 *
 * @author tangliu
 */
public class RuleHolder {

    static Logger logger = LoggerFactory.getLogger(RuleHolder.class);

    private final String username;

    private final String password;

    private boolean doingFresh = false;

    public RuleHolder(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * 缓存project
     */
    private Map<String, ProjectResource> projectResources = new HashMap();

    private static final String settingPath = System.getProperty("rule-conf-path") == null ? "project.setting" : System.getProperty("rule-conf-path") + "project.setting";

    /**
     * 初始化，加载配置文件内容
     */
    public void init() {
        logger.info("开始加载配置文件...");
        Setting setting = new Setting(settingPath);
        String[] projects = setting.getWithLog("project.business.type").split(",");
        logger.info("共找到配置项目：" + JSONObject.toJSONString(projects));

        long start = System.currentTimeMillis();
        for (String project : projects) {
            logger.info("开始加载项目:" + project);
            long start2 = System.currentTimeMillis();
            ProjectResource resource = solveProjectResource(setting, project);
            if (resource != null) {
                addProject(project, resource);
            }
            logger.info("加载项目[{}]完成!耗时 {} ms", project, (System.currentTimeMillis() - start2));
        }
        projectResources.forEach((k, v) -> {
            logger.info("项目[{}]开始启动定时刷新任务", k);
            v.scanStart();
            logger.info("项目[{}]开始启动定时刷新任务完成", k);
        });
        logger.info("加载所有项目完成，耗时 {} ms", (System.currentTimeMillis() - start));
    }

    /**
     * 使用scanner，理解检查规则包是否更新
     */
    public void refreshNow() {
        projectResources.values().forEach(ProjectResource::scanNow);
    }

    /**
     * 刷新配置
     */
    public JSONObject refresh(String businessType) {

        if (doingFresh) {
            logger.warn("规则正在刷新中，请勿重复点击...");
            return ResponseUtil.failed("规则正在刷新中，请勿重复点击...");
        }
        if (businessType == null || "".equals(businessType.trim())) {
            logger.error("规则刷新-项目编号为空");
            return ResponseUtil.failed("规则刷新-项目编号为空");
        }

        ProjectResource projectResource = getProjectResource(businessType);
        if (projectResource == null) {
            logger.error("规则刷新-找不到项目:{}", businessType);
            return ResponseUtil.failed("规则刷新-找不到项目:" + businessType);
        }
        projectResource.scanStop();

        doingFresh = true;
        logger.info("开始刷新配置...");

        Setting setting = new Setting(settingPath);
        String[] projects = setting.getWithLog("project.business.type").split(",");
        logger.info("共找到配置项目：" + JSONObject.toJSONString(projects));

        ProjectResource newProjectResource = null;

        for (String project : projects) {
            if (!project.equals(businessType)) {
                continue;
            }
            logger.info("开始加载项目:" + project);
            long start2 = System.currentTimeMillis();
            newProjectResource = solveProjectResource(setting, project);
            logger.info("加载项目[{}]完成!耗时 {} ms", project, (System.currentTimeMillis() - start2));
        }
        if (newProjectResource != null) {
            addProject(businessType, newProjectResource);
        }
        projectResource.destroy();
        newProjectResource.scanStart();
        doingFresh = false;

        return ResponseUtil.succeed("");
    }


    /**
     * 解析project配置
     *
     * @param setting
     * @param project
     * @return
     */
    private ProjectResource solveProjectResource(Setting setting, String project) {
        try {
            ProjectResource projectResource = new ProjectResource();
            String groupId = setting.getByGroup("groupId", project);
            if (groupId == null) {
                logger.error("project {} 未配置groupId!", project);
                return null;
            }
            String artifactId = setting.getByGroup("artifactId", project);
            if (artifactId == null) {
                logger.error("project {} 未配置artifactId!", project);
                return null;
            }
            String version = setting.getByGroup("version", project);
            if (version == null) {
                logger.error("project {} 未配置version!", project);
                return null;
            }
            //规则数量
            int ruleSize = Integer.parseInt(setting.getByGroupWithLog("rule.size", project));
            for (int i = 1; i <= ruleSize; i++) {
                //规则id
                String ruleId = setting.getByGroup(i + ".id", project);
                /**
                 * 规则名、规则名匹配类型，默认情况下使用规则名前置匹配
                 */
                String ruleName = setting.getByGroup(i + ".name", project) == null ? setting.getByGroup(i + ".id", project) : setting.getByGroup(i + ".name", project);
                String ruleNameType = setting.getByGroup(i + ".name.type", project) == null ? Constants.RULE_NAME_PATTERN_START : setting.getByGroup(i + ".name.type", project);
                /**
                 * 是否插入结果对象
                 */
                boolean ruleInsertResult = setting.getByGroup(i + ".insertResult", project) == null ? Constants.RULE_DEFALT_INSERT_RESULT : Boolean.parseBoolean(setting.getByGroup(i + ".insertResult", project));
                /**
                 * 规则类型：普通规则 or 规则流
                 */
                String ruleType = setting.getByGroup(i + ".type", project) == null ? Constants.RULE_TYPE_NORMAL : setting.getByGroup(i + ".type", project);
                /**
                 * 请求对象
                 */
                String request = setting.getByGroup(i + ".request", project);
                /**
                 * 结果对象
                 */
                String response = setting.getByGroup(i + ".response", project);

                ProjectResource.RuleMethodProperties method = new ProjectResource.RuleMethodProperties(ruleId, ruleName, ruleNameType, ruleType, request, response, ruleInsertResult);
                projectResource.addMethod(ruleId, method);
            }
            projectResource.init(groupId, artifactId, version);
            return projectResource;

        } catch (Exception e) {
            logger.error("加载project[{}]异常!!!", project, e);
            return null;
        }
    }

    /**
     * 获取缓存的project
     *
     * @param projectName
     * @return
     */
    public ProjectResource getProjectResource(String projectName) {
        return projectResources.get(projectName);
    }

    /**
     * 加载project
     *
     * @param projectName
     * @param projectResource
     */
    public void addProject(String projectName, ProjectResource projectResource) {
        this.projectResources.put(projectName, projectResource);
    }
}
