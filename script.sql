CREATE schema dev;

CREATE TABLE dev.film
(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128),
    year_of_release VARCHAR(4),
    genre VARCHAR(32),
    description VARCHAR(128),
    rating INT
);

INSERT INTO dev.film (name, year_of_release, genre, description, rating)
VALUES
    ('Побег из Шоушенка', '1994', 'DRAMA', 'Описание фильма 1', 8),
    ('Крёстный отец', '1972', 'DETECTIVE', 'Описание фильма 2', 7),
    ('Фильм 3', '2022-03-21', 'DRAMA', 'Описание фильма 3', 9),
    ('Фильм 4', '2022-04-10', 'ROMANCE', 'Описание фильма 4', 6),
    ('Фильм 5', '2022-05-05', 'HORROR', 'Описание фильма 5', 8),
    ('Фильм 50', '2022-12-31', 'ACTION', 'Описание фильма 50', 7);


DROP TABLE dev.film;