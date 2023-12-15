package kz.baltabayev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "kz.baltabayev")
public class SpringDemoBotApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringDemoBotApplication.class, args);
	}
}
