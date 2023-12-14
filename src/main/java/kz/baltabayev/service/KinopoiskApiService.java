package kz.baltabayev.service;

import kz.baltabayev.api.KinopoiskApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KinopoiskApiService {

    private final KinopoiskApiClient kinopoiskApiClient;

    public String getMoviesByYearAndGenre(int year, String genre) {
        return kinopoiskApiClient.getMoviesByYearAndGenre(year, genre);
    }

}
