package me.ltang.rules.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步加载
 *
 * @author tangliu
 */
public class AsyncRuleHolder extends RuleHolder implements FactoryBean<RuleHolder> {

    private static Logger logger = LoggerFactory.getLogger(AsyncRuleHolder.class);
    private AtomicBoolean started = new AtomicBoolean(false);

    public AsyncRuleHolder(String username, String password) {
        super(username, password);
    }

    @Override
    public void init() {
        long start = System.currentTimeMillis();
        new Thread(() -> {
            super.init();
            started.compareAndSet(false, true);
            logger.info("异步加载完成，耗时 {} ms", System.currentTimeMillis() - start);
        }).start();
    }

    @Override
    public RuleHolder getObject() throws Exception {
        return this;
    }

    @Override
    public Class<?> getObjectType() {
        return RuleHolder.class;
    }
}
