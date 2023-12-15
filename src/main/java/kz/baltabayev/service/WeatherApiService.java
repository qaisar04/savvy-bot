package kz.baltabayev.service;

import kz.baltabayev.api.WeatherOpenApiClient;
import kz.baltabayev.controller.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherApiService {

    private final WeatherOpenApiClient weather;

    @SneakyThrows
    public void processWeatherRequest(long chatId, String city, TelegramBot bot) {
        String formattedCity = city.replaceFirst(".", Character.toUpperCase(city.charAt(0)) + "");
        String weatherText = weather.getWeather(formattedCity);
        if (weatherText != null && !weatherText.isEmpty()) {
            bot.sendAnswerMessage(chatId, weatherText);
        } else {
            bot.sendAnswerMessage(chatId, "Не удалось получить данные о погоде для указанной локации.😔");
        }
    }

}
