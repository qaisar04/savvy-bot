package kz.baltabayev.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("classpath:application.properties")
public class BotConfig {

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;


}
