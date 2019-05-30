package io.pivotal.weeklyrotator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeeklyRotatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeeklyRotatorApplication.class, args);

    }

}
