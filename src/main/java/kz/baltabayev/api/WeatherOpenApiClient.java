package kz.baltabayev.api;

import kz.baltabayev.translations.WeatherTranslator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@PropertySource("classpath:application.properties")
public class WeatherOpenApiClient {

    @Value("${api.weather}")
    private String API_KEY;

    public static String getUrlContent(String urlAddress) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlAddress))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            return "Ошибка при выполнении вашего запроса. Пожалуйста, попробуйте позже.";
        }
    }

    public String getWeather(String city) {
        String output = getUrlContent(
                "https://api.openweathermap.org/data/2.5/weather?q="
                + city
                + "&appid=" + API_KEY + "&units=metric"
        );
        String weatherText = "Произошла ошибка. Повторите попытку чуть позже!";

        if (!output.isEmpty()) {
            JSONObject object = new JSONObject(output);

            // Main
            JSONObject main = object.getJSONObject("main");
            int temperature = main.getInt("temp");
            int feelsLike = main.getInt("feels_like");
            double pressure = main.getDouble("pressure");
            double humidity = main.getDouble("humidity");

            // Weather
            JSONArray weatherArray = object.getJSONArray("weather");
            JSONObject firstWeatherObject = weatherArray.getJSONObject(0);
            String weatherMain = firstWeatherObject.getString("main");

            String weatherMainTranslated = WeatherTranslator.translateWeatherMain(weatherMain);

            // Wind
            JSONObject wind = object.optJSONObject("wind");
            double windSpeed = wind.getDouble("speed");

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

        return weatherText;
    }
}
