package kz.baltabayev.controller;

import jakarta.annotation.PostConstruct;
import kz.baltabayev.entity.Feedback;
import kz.baltabayev.entity.Security;
import kz.baltabayev.entity.User;
import kz.baltabayev.entity.type.BotState;
import kz.baltabayev.service.*;
import kz.baltabayev.util.DateTimeUtils;
import kz.baltabayev.util.FeedbackFormatterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final SecurityService security;
    private final TelegramCommandService command;
    private final WeatherApiService weather;
    private final FeedbackService feedback;
    private final UserService userService;

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        String messageText = message.getText().replace("@SavvyTelegramBot", "");
        long chatId = message.getChatId(); // идендификатор чата
        long userId = message.getFrom().getId(); // идендификатор пользователя, который отправил сообщение

        String chatName = Optional.ofNullable(message.getChat().getUserName())
                .orElse(message.getChat().getFirstName());

        String username = Optional.ofNullable(message.getFrom().getUserName())
                .orElse(message.getFrom().getFirstName());

        User user = userService.getUserByUserIdAndChatId(userId, chatId)
                .orElse(User.builder()
                        .username(username)
                        .chatId(chatId)
                        .userId(userId)
                        .botState(BotState.WAITING_FOR_MESSAGE)
                        .build());

        userService.save(user);

        log.info("from: {} | chat: {} | chatId: {} | userId: {} | {}", username, chatName, chatId, userId, messageText);

        if (messageText.equals("/exit")) {
            handleExitCommand(user);
        } else if (user.getBotState().equals(BotState.WAITING_FOR_MESSAGE) && user.getUserId() != null
                   && user.getUserId().equals(userId)) {
            handleCommand(messageText, user);
        } else {
            handleUserInput(messageText, user);
        }
    }

    @PostConstruct
    public void init() {
        List<Long> uniqueChatId = userService.getUniqueChatId();
        List<Long> uniqueUserId = userService.getUniqueUserId();

        Set<Long> uniqueIds = new HashSet<>();
        uniqueIds.addAll(uniqueChatId);
        uniqueIds.addAll(uniqueUserId);

        for (Long chatId : uniqueIds) {
            sendAnswerMessage(chatId, "Был технический перерыв, но теперь я в строю и работаю еще лучше!");
        }
    }

    private void handleUserInput(String messageText, User user) {
        BotState currentState = user.getBotState();

        if (currentState == null) {
            user.setBotState(BotState.WAITING_FOR_MESSAGE);
            sendAnswerMessage(user.getChatId(), "Пожалуйста, повторите попытку.");
        } else {
            switch (currentState) {
                case WAITING_FOR_CITY -> handleCityInput(messageText, user);
                case WAITING_FOR_FEEDBACK -> handleFeedbackInput(messageText, user);
                case WAITING_FOR_ADMIN -> handleAdminInput(messageText, user);
                default -> {
                    sendAnswerMessage(user.getChatId(), "Неожиданное состояние бота. Команда для выхода /exit");
                }
            }
        }
    }

    private void handleCityInput(String messageText, User user) {
        weather.processWeatherRequest(user.getChatId(), messageText, this);
        user.setBotState(BotState.WAITING_FOR_MESSAGE);
        userService.save(user);
    }

    private void handleFeedbackInput(String messageText, User user) {
                Feedback feedbackUser = Feedback.builder()
                .username(user.getUsername())
                .createdAt(DateTimeUtils.parseDateTime(LocalDateTime.now()))
                .description(messageText)
                .build();
        feedback.save(feedbackUser);
        sendAnswerMessage(user.getChatId(), "Спасибо за ваше участие и помощь в оценке нашей работы. Желаем вам отличного настроения и приятного пользования нашими услугами!");
        user.setBotState(BotState.WAITING_FOR_MESSAGE);
        userService.save(user);
    }

    private void handleAdminInput(String messageText, User user) {
        Optional<Security> securityOptional = security.findByUuidCode(messageText);
        if (securityOptional.isPresent()) {
            LocalDateTime createdAt = DateTimeUtils.parseDateTimeFromString(securityOptional.get().getCreatedAt());
            LocalDateTime oneMinutesAgo = LocalDateTime.now().minusMinutes(1);

            if (createdAt.isBefore(oneMinutesAgo)) {
                security.delete(securityOptional.get());
                sendAnswerMessage(user.getChatId(), "Время действия кода истекло. Пожалуйста, запросите новый код.");
            } else {
                user.setBotState(BotState.WAITING_FOR_MESSAGE);
                userService.save(user);
                security.delete(securityOptional.get());
                List<Feedback> feedbackList = feedback.findAll();
                sendAnswerMessage(user.getChatId(), FeedbackFormatterUtils.formatFeedbackList(feedbackList));
            }
        } else {
            sendAnswerMessage(user.getChatId(), "Введен неверный код. Пожалуйста, проверьте ваши данные и повторите попытку. Команда для выхода /exit");
        }
    }

    private void handleCommand(String messageText, User user) {
        switch (messageText) {
            case "/start" -> {
                command.startCommandReceived(user.getChatId(), user.getUsername(), this);
                user.setBotState(BotState.WAITING_FOR_MESSAGE);
                userService.save(user);
            }
            case "/exit" -> handleExitCommand(user);
            case "/coinflip" -> command.coinFlipCommandReceived(user.getChatId(), this);
            case "/help" -> command.showHelpCommandReceived(user.getChatId(), this);
            case "/weather" -> handleWeatherCommand(user.getChatId(), user);
            case "/feedback" -> handleFeedbackCommand(user.getChatId(), user);
            case "/admin" -> handleAdminCommand(user.getChatId(), user);
            default -> handleUnknownCommand(user.getChatId());
        }
    }

    private void handleExitCommand(User user) {
        if (user.getBotState().equals(BotState.WAITING_FOR_MESSAGE)) {
            sendAnswerMessage(user.getChatId(), "Вы уже находитесь в режиме ожидания.");
        }
        user.setBotState(BotState.WAITING_FOR_MESSAGE);
        userService.save(user);
        sendAnswerMessage(user.getChatId(), "Вы успешно вышли из текущего режима.");
    }

    private void handleWeatherCommand(long chatId, User user) {
        if(user.getUserId() != null && user.getChatId() != null && user.getChatId().equals(chatId)) {
            sendAnswerMessage(chatId, "Введите локацию, для которого вы хотите узнать погоду. Команда для выхода /exit");
            user.setBotState(BotState.WAITING_FOR_CITY);
            userService.save(user);
        }
    }

    private void handleFeedbackCommand(long chatId, User user) {
        if (security.isPrivateChat(chatId, this)) {
            sendAnswerMessage(chatId, "Пожалуйста, поделитесь своим мнением. Команда для выхода /exit");
            user.setBotState(BotState.WAITING_FOR_FEEDBACK);
            userService.save(user);
        }
    }

    private void handleAdminCommand(long chatId, User user) {
        if (security.isPrivateChat(chatId, this)) {
            security.sendConfirmationCode(this);
            sendAnswerMessage(chatId, "Для завершения процесса, введите код подтверждения, отправленный на аккаунт владельца. Обратите внимание, что токен активен в течение 60 секунд. После истечения этого времени токен будет автоматически удален. Вернуться в главное меню можно с помощью команды /exit.");
            user.setBotState(BotState.WAITING_FOR_ADMIN);
            userService.save(user);
        }
    }

    private void handleUnknownCommand(long chatId) {
        if (chatId > 0) {
            sendAnswerMessage(chatId, "К сожалению я не знаю такой команды, данный запрос я сохранил в базу данных!");
        }
    }

    public void sendAnswerMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    public void sendPhotoMessage(long chatId, String photoPath) {
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

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

}
