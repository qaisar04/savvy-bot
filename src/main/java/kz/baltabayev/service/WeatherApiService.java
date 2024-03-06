package kz.baltabayev.service;

import kz.baltabayev.api.WeatherOpenApiClient;
import kz.baltabayev.controller.TelegramBot;
import kz.baltabayev.entity.MessageText;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherApiService {

    private final WeatherOpenApiClient weather;

    @SneakyThrows
    public void processWeatherRequest(long chatId, MessageText city, TelegramBot bot) {
        String formattedCity = city.getMessageText().replaceFirst(".", Character.toUpperCase(city.getMessageText().charAt(0)) + "");
        String weatherText = weather.getWeather(formattedCity);
        bot.deleteMessageWithTimer(city.getChatId(), city.getMessageId(), 30);
        if (weatherText != null && !weatherText.isEmpty()) {
            bot.sendAnswerMessage(chatId, weatherText, 30);
        } else {
            bot.sendAnswerMessage(chatId, "Не удалось получить данные о погоде для указанной локации.😔", 30);
        }
    }

}
