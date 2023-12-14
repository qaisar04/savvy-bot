package kz.baltabayev.controller;

import kz.baltabayev.api.WeatherOpenApiClient;
import kz.baltabayev.config.BotConfig;
import kz.baltabayev.entity.Feedback;
import kz.baltabayev.entity.Security;
import kz.baltabayev.entity.type.BotState;
import kz.baltabayev.repository.FeedbackRepository;
import kz.baltabayev.repository.SecurityRepository;
import kz.baltabayev.service.KinopoiskApiService;
import kz.baltabayev.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final WeatherOpenApiClient weatherClient;
    private final FeedbackRepository feedback;
    private final SecurityRepository security;

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
            Long storedUserId = userStateMap.get(chatId);

            String username = Optional.ofNullable(update.getMessage().getChat().getUserName())
                    .orElse(update.getMessage().getChat().getFirstName());
            log.info("{} | {} | {}", username, messageText, chatId);

            if (storedUserId != null && storedUserId.equals(userId) && !messageText.equals("/exit")) {
                switch (botStateMap.get(chatId)) {
                    case WAITING_FOR_CITY -> {
                        botStateMap.put(chatId, BotState.WAITING_FOR_MESSAGE);
                        userStateMap.remove(chatId);
                        processWeatherRequest(chatId, messageText);
                    }
                    case WAITING_FOR_FEEDBACK -> {
                        botStateMap.put(chatId, BotState.WAITING_FOR_MESSAGE);
                        userStateMap.remove(chatId);
                        Feedback feedbackUser = Feedback.builder()
                                .username(username)
                                .createdAt(DateTimeUtils.parseDateTime(LocalDateTime.now()))
                                .description(messageText)
                                .build();
                        feedback.save(feedbackUser);
                        sendAnswerMessage(chatId, "Спасибо за ваше участие и помощь в оценке нашей работы. Желаем вам отличного настроения и приятного пользования нашими услугами!");

                    }
                    case WAITING_FOR_ADMIN -> {
                        Optional<Security> securityOptional = security.findByUuidCode(messageText);

                        if (securityOptional.isPresent()) {
                            botStateMap.put(chatId, BotState.WAITING_FOR_MESSAGE);
                            userStateMap.remove(chatId);
                            List<Feedback> feedbackList = feedback.findAll();
                            sendAnswerMessage(chatId, formatFeedbackList(feedbackList));
                            security.delete(securityOptional.get());
                        } else {
                            sendAnswerMessage(chatId, "Введен неверный код. Пожалуйста, проверьте ваши данные и повторите попытку. Команда для выхода /exit");
                        }
                    }
                }
            } else {
                switch (messageText) {
                    case "/start" -> {
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    }
                    case "/exit" -> {
                        botStateMap.put(chatId, BotState.WAITING_FOR_MESSAGE);
                        userStateMap.remove(chatId);
                        sendAnswerMessage(chatId, "Вы успешно вышли из текущего режима.");
                    }
                    case "/coinflip" -> {
                        coinFlipCommandReceived(chatId);
                    }
                    case "/help" -> {
                        showHelpCommandReceived(chatId);
                    }
                    case "/weather" -> {
                        sendAnswerMessage(chatId, "Введите локацию, для которого вы хотите узнать погоду. Команда для выхода /exit");
                        botStateMap.put(chatId, BotState.WAITING_FOR_CITY);
                        userStateMap.put(chatId, userId);
                    }
                    case "/kino" -> {
                        sendAnswerMessage(chatId, "Данная команда на обработке.");
                    }
                    case "/feedback" -> {
                        if (chatId > 0) {
                            sendAnswerMessage(chatId, "Пожалуйста, поделитесь своим мнением. Команда для выхода /exit");
                            botStateMap.put(chatId, BotState.WAITING_FOR_FEEDBACK);
                            userStateMap.put(chatId, userId);
                        } else {
                            sendAnswerMessage(chatId, "Пожалуйста, применяйте указанную команду в личных сообщениях.");
                        }
                    }
                    case "/admin" -> {
                        sendConfirmationCode();
                        sendAnswerMessage(chatId, "Для завершения процесса, введите код подтверждения, отправленный на аккаунт владельца. Команда для выхода /exit");
                        botStateMap.put(chatId, BotState.WAITING_FOR_ADMIN);
                        userStateMap.put(chatId, userId);
                    }
                    default -> {
                        if (chatId > 0) {
                            sendAnswerMessage(chatId, "К сожалению я не знаю такой команды, данный запрос я сохранил в базу данных!");
                        }
                    }
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

    @SneakyThrows
    private void processWeatherRequest(long chatId, String city) {
        String formattedCity = city.replaceFirst(".", Character.toUpperCase(city.charAt(0)) + "");
        String weatherText = weatherClient.getWeather(formattedCity);
        if (weatherText != null && !weatherText.isEmpty()) {
            sendAnswerMessage(chatId, weatherText);
        } else {
            log.error("Failed to fetch weather data for city: " + formattedCity);
            sendAnswerMessage(chatId, "Не удалось получить данные о погоде для указанной локации.😔");
        }
    }

    private void showHelpCommandReceived(long chatId) {
        String helpMessage = "Список доступных команд:\n" +
                             "/coinflip - подбросить монетку\n" +
                             "/weather - узнать информацию о погоде\n" +
                             "/help - показать список доступных команд\n" +
                             "/feedback - оставить обратную связь о боте\n\n" +
                             "группа в телеграмме: @prgrm_java";

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

    private void sendConfirmationCode() {
        String uuidCode = generateUUID();
        Security build = Security.builder()
                .uuidCode(uuidCode)
                .build();
        sendAnswerMessage(697119914, uuidCode);
        security.save(build);
    }

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }


    private String formatFeedbackList(List<Feedback> feedbackList) {
        StringBuilder formattedList = new StringBuilder();

        for (Feedback feedback : feedbackList) {
            formattedList.append("ID: ").append(feedback.getId())
                    .append(", Username: ").append(feedback.getUsername())
                    .append(", Created At: ").append(feedback.getCreatedAt())
                    .append(", Description: ").append(feedback.getDescription())
                    .append("\n");
        }

        return formattedList.toString();
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
