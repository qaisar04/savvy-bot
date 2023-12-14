package kz.baltabayev.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KinopoiskApiClient {

    @Value("${bot.kinopoisk_token}")
    private String API_KEY;

    private RestTemplate restTemplate;

    public String getMoviesByYearAndGenre(int year, String genre) {
        String url = "https://api.kinopoisk.dev/v1.4/movie?year=" + year + "&genres.name=" + genre;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", API_KEY);


        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, headers);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return "Ошибка запроса: " + response.getStatusCodeValue();
        }

    }

}
