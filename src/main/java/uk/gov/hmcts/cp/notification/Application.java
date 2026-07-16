package uk.gov.hmcts.cp.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("uk.gov.hmcts.cp")
@EnableScheduling
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
