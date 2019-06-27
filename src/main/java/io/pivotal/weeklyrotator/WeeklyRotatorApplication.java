package io.pivotal.weeklyrotator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

@SpringBootApplication
@EnableScheduling
public class WeeklyRotatorApplication {

    public static void main(String[] args) {
        String[] disabledCommands = {"--spring.shell.command.quit.enabled=false"};
        String[] fullArgs = StringUtils.concatenateStringArrays(args, disabledCommands);
        SpringApplication.run(WeeklyRotatorApplication.class, fullArgs);

    }

}
