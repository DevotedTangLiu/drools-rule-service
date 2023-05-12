package me.ltang.rules.junit;

import me.ltang.rules.DroolsApplication;
import me.ltang.rules.config.RuleProjectsLoadConfig;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;


@ContextConfiguration(classes = {RuleProjectsLoadConfig.class})
@ComponentScan("book.framework.engine.drools")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DroolsApplication.class)
public class DroolsJunitTest {
}
