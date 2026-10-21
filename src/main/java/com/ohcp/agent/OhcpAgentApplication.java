package com.ohcp.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** OHCP 农作物缺素症识别与科学施肥建议智能体启动入口 */
@SpringBootApplication
public class OhcpAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OhcpAgentApplication.class, args);
    }
}