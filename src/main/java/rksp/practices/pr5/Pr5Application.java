package rksp.practices.pr5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.rsocket.RSocketServerAutoConfiguration;

@SpringBootApplication(exclude = RSocketServerAutoConfiguration.class)
public class Pr5Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Pr5Application.class);
        app.setWebApplicationType(WebApplicationType.REACTIVE);
        app.run(args);
    }
}
