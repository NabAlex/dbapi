package app;

import app.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static Boolean localDebugMode = true;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
