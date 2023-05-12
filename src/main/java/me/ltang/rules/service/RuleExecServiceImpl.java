package me.ltang.rules.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.ltang.rules.config.ProjectResource;
import me.ltang.rules.config.RuleHolder;
import me.ltang.rules.util.Constants;
import me.ltang.rules.util.RuleException;
import org.drools.core.ClassObjectFilter;
import org.drools.core.base.RuleNameEndsWithAgendaFilter;
import org.drools.core.base.RuleNameEqualsAgendaFilter;
import org.drools.core.base.RuleNameMatchesAgendaFilter;
import org.drools.core.base.RuleNameStartsWithAgendaFilter;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.kie.api.KieBase;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

import static me.ltang.rules.util.Constants.*;

/**
 * @author tangliu
 */
@Service
public class RuleExecServiceImpl {

    final static Logger logger = LoggerFactory.getLogger(RuleExecServiceImpl.class);

    @Autowired
    RuleHolder ruleHolder;

    /**
     * 构造请求参数，请求规则引擎
     *
     * @param businessType
     * @param ruleId
     * @param params
     * @param resultObj
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Object execRule(String businessType, String ruleId, JSONObject params, JSONObject resultObj) throws RuleException {

        ProjectResource projectResource = ruleHolder.getProjectResource(businessType);
        if (projectResource == null) {
            logger.error("找不到项目:{}", businessType);
            throw new RuleException("找不到项目");
        }
        ProjectResource.RuleMethodProperties method = projectResource.getMethod(ruleId);
        if (method == null) {
            logger.error("找不到规则:{}", ruleId);
            logger.info("使用默认规则配置:");
            method = new ProjectResource.RuleMethodProperties(ruleId, ruleId, RULE_NAME_PATTERN_START, RULE_TYPE_NORMAL, DEFAULT_INSERT_REQUEST_CLAZZ, DEFAULT_RESPONSE_CLAZZ, true);
            logger.info(JSONObject.toJSONString(method));
        }
        //获取规则相关参数
        String reqFactClassName = method.getRequest();
        String rspFactClassName = method.getResponse();
        String ruleType = method.getRuleType();
        String ruleName = method.getRuleName();
        String ruleNameType = method.getRuleNameType();
        boolean ruleInsertResult = method.isInsertResult();

        if (ruleInsertResult && rspFactClassName == null) {
            throw new RuleException("规则[{}]配置了需要插入结果数据对象，但未定义数据对象！", ruleId);
        }

        KieSession session = null;
        FactHandle reqFactHandler = null;
        FactHandle rspFactHandler = null;
        try {
            //反射构造请求数据对象和结果数据对象
            KieBase kieBase = projectResource.getKieBase();
            Class<?> reqClazz = ((KnowledgeBaseImpl) kieBase).getClassFieldAccessorCache().getClassLoader().loadClass(reqFactClassName);
            Class<?> rspClazz = null;
            Object reqObj = params.toJavaObject(reqClazz);
            Object rspObj = null;

            session = kieBase.newKieSession();
            //插入请求数据对象
            reqFactHandler = session.insert(reqObj);
            //插入结果数据对象
            if (ruleInsertResult) {
                rspClazz = ((KnowledgeBaseImpl) kieBase).getClassFieldAccessorCache().getClassLoader().loadClass(rspFactClassName);
                rspObj = rspClazz.newInstance();
                rspFactHandler = session.insert(rspObj);
            }
            //普通规则 & 规则流
            if (RULE_TYPE_NORMAL.equalsIgnoreCase(ruleType)) {
                AgendaFilter nameFilter;
                switch (ruleNameType) {
                    case RULE_NAME_PATTERN_START:
                        nameFilter = new RuleNameStartsWithAgendaFilter(ruleName);
                        break;
                    case Constants.RULE_NAME_PATTERN_END:
                        nameFilter = new RuleNameEndsWithAgendaFilter(ruleName);
                        break;
                    case Constants.RULE_NAME_PATTERN_MATCH:
                        nameFilter = new RuleNameMatchesAgendaFilter(ruleName);
                        break;
                    default:
                        nameFilter = new RuleNameEqualsAgendaFilter(ruleName);
                        break;
                }
                int times = session.fireAllRules(nameFilter);
                logger.info("共触发规则'{}' {}次", ruleName, times);
                resultObj.put("times", times);

            } else if (Constants.RULE_TYPE_FLOW.equalsIgnoreCase(ruleType)) {
                session.startProcess(ruleName);
                logger.info("已触发规则流:" + ruleName);

            } else {
                throw new RuleException("不支持的规则类型!");
            }
            logger.info("请求体：" + JSON.toJSONString(reqObj));

            //如果是插入的结果数据对象，直接返回该对象即可
            if (ruleInsertResult) {
                logger.info("返回体：" + JSON.toJSONString(rspObj));
                return rspObj;
            }
            //否则，判断是否需要取回结果数据对象，若需要，则反射获取，不需要，则返回请求体
            if (rspFactClassName != null) {
                rspClazz = ((KnowledgeBaseImpl) kieBase).getClassFieldAccessorCache().getClassLoader().loadClass(rspFactClassName);
                Collection<?> results = session.getObjects(new ClassObjectFilter(rspClazz));
                if (results != null && results.size() > 0) {
                    for (Object result : results) {
                        logger.info("返回体：" + JSON.toJSONString(result));
                    }
                    return results.toArray()[0];
                } else {
                    logger.error("没找到返回体，也可能是没有命中规则");
                    throw new RuleException("没找到返回体，也可能是没有命中规则");
                }
            }
            return reqObj;
        } catch (Exception e) {
            logger.error("规则执行异常:", e);
            throw new RuleException("规则执行异常!" + e.getMessage() + "===>" + e.getCause().toString());
        } finally {
            if (session != null) {
                if (reqFactHandler != null) {
                    session.delete(reqFactHandler);
                }
                if (rspFactHandler != null) {
                    session.delete(rspFactHandler);
                }
                session.dispose();
            }
        }
    }


    /**
     * 根据包名和类名构建一个类
     *
     * @param base
     * @param packageName
     * @param className
     * @return
     */
    protected static FactType initfactType(KieBase base, String packageName, String className) {
        FactType factType = base.getFactType(packageName, className);
        return factType;
    }

    /**
     * 使用请求参数对实体赋值，目前仅支持简单Map结构
     *
     * @param factType
     * @param params
     * @return
     */
    protected static Object fillFactType(FactType factType, Map<String, Object> params) throws IllegalAccessException, InstantiationException {
        Object obj = factType.newInstance();
        factType.setFromMap(obj, params);
        return obj;
    }

    protected static Object billObject(String className, JSONObject params) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        Object obj = params.toJavaObject(clazz);
        return obj;
    }
}
