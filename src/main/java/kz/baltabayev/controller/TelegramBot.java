package kz.baltabayev.controller;

import kz.baltabayev.api.WeatherOpenApiClient;
import kz.baltabayev.config.BotConfig;
import kz.baltabayev.domain.type.BotState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final WeatherOpenApiClient weatherClient;

    private final Map<Long, BotState> botStateMap = new HashMap<>();
    private final Map<Long, Long> userStateMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            Message message = update.getMessage();
            String messageText = message.getText();
            messageText = messageText.replace("@SavvyTelegramBot", "");

            long chatId = message.getChatId();
            long userId = message.getFrom().getId();

            String username = Optional.ofNullable(update.getMessage().getChat().getUserName())
                    .orElse(update.getMessage().getChat().getFirstName());
            log.info(username + " | " + messageText + " | " + chatId);

            Long storedUserId = userStateMap.get(chatId);
            if (storedUserId != null && storedUserId.equals(userId) && botStateMap.get(chatId) == BotState.WAITING_FOR_CITY) {
                botStateMap.put(chatId, BotState.WAITING_FOR_MESSAGE);
                userStateMap.remove(chatId);
                processWeatherRequest(chatId, messageText);
            } else {
                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/coinflip":
                        coinFlipCommandReceived(chatId);
                        break;
                    case "/help":
                        showHelpCommandReceived(chatId);
                        break;
                    case "/weather":
                        sendAnswerMessage(chatId, "Введите локацию, для которого вы хотите узнать погоду:");
                        botStateMap.put(chatId, BotState.WAITING_FOR_CITY);
                        userStateMap.put(chatId, userId);
                        break;
                    default:
                        sendAnswerMessage(chatId, "К сожалению я не знаю такой команды, данный запрос я сохранил в базу данных!");
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    private void processWeatherRequest(long chatId, String city) {
        String weatherText = weatherClient.getWeather(city);

        if (weatherText != null && !weatherText.isEmpty()) {
            sendAnswerMessage(chatId, weatherText);
        } else {
            log.error("Failed to fetch weather data for city: " + city);
            sendAnswerMessage(chatId, "Не удалось получить данные о погоде для указанной локации.😔");
        }
    }

    private void showHelpCommandReceived(long chatId) {
        String helpMessage = "Список доступных команд:\n" +
                             "/coinflip - Подбросить монетку\n" +
                             "/help - Показать список доступных команд\n" +
                             "/weather - Узнать информацию о погоде\n\n" +
                             "Группа в телеграмме: @prgrm_java";

        sendAnswerMessage(chatId, helpMessage);
    }

    private void coinFlipCommandReceived(long chatId) {
        String[] coinSides = {"орёл 🦅", "решка 💰"};
        int randomIndex = ThreadLocalRandom.current().nextInt(coinSides.length);
        String result = coinSides[randomIndex];


        String answer = String.format("Монетка была подброшена... и она выпала на: %s", result);
        sendAnswerMessage(chatId, answer);
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ". Рад видеть вас!\n" +
                        "/help - Показать список доступных команд";
        log.info("Replide to user " + name);
        sendAnswerMessage(chatId, answer);
    }

    private void sendAnswerMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    private void sendPhotoMessage(long chatId, String photoPath) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));

        InputFile photo = new InputFile(new File(photoPath));
        sendPhoto.setPhoto(photo);

        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

}
