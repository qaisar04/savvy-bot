package kz.baltabayev.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.baltabayev.translations.WeatherTranslator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class WeatherOpenApiClient {

    @Value("${bot.weather_token}")
    private String API_KEY;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public String getWeather(String city) throws JsonProcessingException {
        String output = ApiClient.getUrlContent(
                "https://api.openweathermap.org/data/2.5/weather?q="
                + city
                + "&appid=" + API_KEY + "&units=metric"
        );

        JsonNode jsonNode = objectMapper.readTree(output);
        String weatherText = "Произошла ошибка. Повторите попытку чуть позже!";

        if (!output.isEmpty()) {
            JsonNode main = jsonNode.get("main");
            if (main != null) {
                int temperature = main.get("temp").asInt();
                int feelsLike = main.get("feels_like").asInt();
                double pressure = main.get("pressure").asDouble();
                double humidity = main.get("humidity").asDouble();

                JsonNode weatherArray = jsonNode.get("weather");
                if (weatherArray != null && weatherArray.isArray() && weatherArray.size() > 0) {
                    JsonNode weather = weatherArray.get(0);
                    String weatherMain = weather.get("main").asText();

                    String weatherMainTranslated = WeatherTranslator.translateWeatherMain(weatherMain);

                    JsonNode wind = jsonNode.get("wind");
                    double windSpeed = wind.get("speed").asDouble();


                    if (temperature <= 0) {
                        weatherText = "❄️☃️ Холодная погода в регионе " + city + " :\n" +
                                      "Температура: " + temperature + "°C 🥶\n" +
                                      "Ощущается как: " + feelsLike + "°C \n" +
                                      "Состояние погоды: " + weatherMainTranslated + "\n" +
                                      "Влажность: " + humidity + "% 💧\n" +
                                      "Давление: " + pressure + " hPa 🧭\n" +
                                      "Скорость ветра: " + windSpeed + " м/с 🌬️";
                    } else if (temperature < 20) {
                        weatherText = "⛅️ Погода в регионе " + city + ":\n" +
                                      "Температура: " + temperature + "°C 🌤️\n" +
                                      "Ощущается как: " + feelsLike + "°C \n" +
                                      "Состояние погоды: " + weatherMainTranslated + "\n" +
                                      "Влажность: " + humidity + "% 💧\n" +
                                      "Давление: " + pressure + " hPa 🧭\n" +
                                      "Скорость ветра: " + windSpeed + " м/с 🌬️";
                    } else {
                        weatherText = "☀️ Солнечная погода в регионе " + city + ":\n" +
                                      "Температура: " + temperature + "°C 🥵\n" +
                                      "Ощущается как: " + feelsLike + "°C \n" +
                                      "Состояние погоды: " + weatherMainTranslated + "\n" +
                                      "Влажность: " + humidity + "% 💧\n" +
                                      "Давление: " + pressure + " hPa 🧭\n" +
                                      "Скорость ветра: " + windSpeed + " м/с 🌬️";
                    }
                }
            }
        }

        return weatherText;
    }
}

