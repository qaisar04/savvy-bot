package kz.baltabayev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "kz.baltabayev")
public class SpringBotApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringBotApplication.class, args);
	}
}
