package me.ltang.rules;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author tangliu
 */
@SpringBootApplication
public class DroolsApplication {
    public static void main(String[] args) {

        String profile = System.getProperty("spring.profiles.active");
        if (profile == null) {
            System.setProperty("spring.profiles.active", "dev");
        }
        SpringApplication.run(DroolsApplication.class, args);

        System.out.println("规则服务启动成功!\n" +
                "\n" +
                "##       ########    ###    ##    ##  ######       ##     ## ######## \n" +
                "##          ##      ## ##   ###   ## ##    ##      ###   ### ##       \n" +
                "##          ##     ##   ##  ####  ## ##            #### #### ##       \n" +
                "##          ##    ##     ## ## ## ## ##   ####     ## ### ## ######   \n" +
                "##          ##    ######### ##  #### ##    ##      ##     ## ##       \n" +
                "##          ##    ##     ## ##   ### ##    ##  ### ##     ## ##       \n" +
                "########    ##    ##     ## ##    ##  ######   ### ##     ## ######## \n");
    }

}
