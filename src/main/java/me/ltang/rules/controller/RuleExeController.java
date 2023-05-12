package me.ltang.rules.controller;

import com.alibaba.fastjson.JSONObject;
import me.ltang.rules.config.RuleHolder;
import me.ltang.rules.model.RuleRequest;
import me.ltang.rules.service.RuleExecServiceImpl;
import me.ltang.rules.util.ResponseUtil;
import me.ltang.rules.util.RuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供接口给外部系统调用，执行指定的规则（流）
 */
@RestController
@RequestMapping("/rule")
public class RuleExeController {

    final static Logger logger = LoggerFactory.getLogger(RuleExeController.class);

    @Autowired
    RuleExecServiceImpl ruleExecService;

    @Autowired
    RuleHolder ruleHolder;

    /**
     * 规则执行接口，供外部系统调用
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/exec")
    public JSONObject exec(@RequestBody RuleRequest request) throws RuleException {
        long start = System.currentTimeMillis();
        logger.info("请求REQ:" + JSONObject.toJSONString(request));
        try {
            JSONObject result = new JSONObject();
            Object rsp = ruleExecService.execRule(request.getBusinessType(), request.getRuleId(), request.getParams(), result);
            result = ResponseUtil.succeed(result, rsp);
            logger.info("请求RSP:" + result.toString());
            logger.info("请求处理耗时:{} ms", (System.currentTimeMillis() - start));
            return result;
        } catch (RuleException e) {
            logger.error("请求规则服务异常:" + e.getMsg());
            return ResponseUtil.failed(e);
        }
    }

    /**
     * 刷新配置，慎用
     * <p>
     * 当修改了配置文件，可以使用此url刷新配置
     *
     * @return
     */
    @PostMapping(value = "/exec/refreshConf")
    public JSONObject refreshConf(@RequestBody RuleRequest request) {
        logger.info("收到刷新配置请求:" + JSONObject.toJSONString(request));
        return ruleHolder.refresh(request.getBusinessType());
    }

    /**
     * 调用scanner，判断规则是否有更新，有更新则更新
     *
     * @return
     */
    @RequestMapping(value = "/exec/refreshNow")
    public JSONObject refreshTest() {
        ruleHolder.refreshNow();
        return ResponseUtil.succeed("");
    }
}
