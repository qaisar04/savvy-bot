package kz.baltabayev.translations;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WeatherTranslator {

    private static final Map<String, String> weatherMainTranslations;

    static {
        weatherMainTranslations = new HashMap<>();
        weatherMainTranslations.put("Clear", "ясно");
        weatherMainTranslations.put("Clouds", "облачно");
        weatherMainTranslations.put("Thunderstorm", "гроза");
        weatherMainTranslations.put("Drizzle", "мелкий дождь");
        weatherMainTranslations.put("Rain", "дождь");
        weatherMainTranslations.put("Snow", "снег");
        weatherMainTranslations.put("Mist", "дым");
        weatherMainTranslations.put("Haze", "пыль");
        weatherMainTranslations.put("Fog", "туман");
        weatherMainTranslations.put("Sand", "песчаная отмель");
        weatherMainTranslations.put("Dust", "пыль");
        weatherMainTranslations.put("Ash", "вулканический пепел");
        weatherMainTranslations.put("Squall", "шквал");
        weatherMainTranslations.put("Tornado", "торнадо");
    }

    public static String translateWeatherMain(String weatherMain) {
        return weatherMainTranslations.getOrDefault(weatherMain, weatherMain);
    }

}
