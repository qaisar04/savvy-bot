package kz.baltabayev.controller;

import jakarta.annotation.PostConstruct;
import kz.baltabayev.entity.Feedback;
import kz.baltabayev.entity.MessageText;
import kz.baltabayev.entity.Security;
import kz.baltabayev.entity.User;
import kz.baltabayev.entity.type.BotState;
import kz.baltabayev.entity.type.MessageState;
import kz.baltabayev.service.*;
import kz.baltabayev.util.DateTimeUtils;
import kz.baltabayev.util.FeedbackFormatterUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final SecurityService security;
    private final TelegramCommandService command;
    private final WeatherApiService weather;
    private final FeedbackService feedback;
    private final UserService userService;
    private final String botName;
    private final ScheduledExecutorService scheduler;

    @Autowired
    public TelegramBot(SecurityService security, TelegramCommandService command, WeatherApiService weather, FeedbackService feedback, UserService userService, @Value("${bot.name}") String botName, @Value("${bot.token}") String botToken, @Lazy ScheduledExecutorService scheduler) {
        super(botToken);
        this.security = security;
        this.command = command;
        this.weather = weather;
        this.feedback = feedback;
        this.userService = userService;
        this.botName = botName;
        this.scheduler = scheduler;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(5);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        MessageText messageText = checkMessage(message);

        String username = Optional.ofNullable(message.getFrom().getUserName())
                .orElse(message.getFrom().getFirstName());

        User user = userService.getUserByUserIdAndChatId(messageText.getUserId(), messageText.getChatId())
                .orElse(User.builder()
                        .username(username)
                        .chatId(messageText.getChatId())
                        .userId(messageText.getUserId())
                        .botState(BotState.WAITING_FOR_MESSAGE)
                        .build());
        userService.save(user);

        if (messageText.getMessageText().equals("/exit")) {
            handleExitCommand(user);
        } else if (user.getBotState().equals(BotState.WAITING_FOR_MESSAGE) && user.getUserId() != null
                   && user.getUserId().equals(messageText.getUserId())) {
            handleCommand(messageText, user);
        } else {
            handleUserInput(messageText, user);
        }
    }

    private void handleUserInput(MessageText messageText, User user) {
        BotState currentState = user.getBotState();

        if (currentState == null) {
            user.setBotState(BotState.WAITING_FOR_MESSAGE);
            sendAnswerMessage(user.getChatId(), "Пожалуйста, повторите попытку.");
        } else {
            switch (currentState) {
                case WAITING_FOR_CITY -> handleCityInput(messageText, user);
                case WAITING_FOR_FEEDBACK -> handleFeedbackInput(messageText.getMessageText(), user);
                case WAITING_FOR_ADMIN -> handleAdminInput(messageText.getMessageText(), user);
                default -> {
                    sendAnswerMessage(user.getChatId(), "Неожиданное состояние бота. Команда для выхода /exit");
                }
            }
        }
    }

    private void handleCityInput(MessageText messageText, User user) {
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

    private void handleCommand(MessageText messageText, User user) {
        switch (messageText.getMessageText()) {
            case "/start" -> {
                command.startCommandReceived(user.getChatId(), user.getUsername(), this);
                user.setBotState(BotState.WAITING_FOR_MESSAGE);
                userService.save(user);
            }
            case "/exit" -> handleExitCommand(user);
            case "/coinflip" -> command.coinFlipCommandReceived(user.getChatId(), this);
            case "/help" -> command.showHelpCommandReceived(user.getChatId(), this);
            case "/weather" -> handleWeatherCommand(user.getChatId(), user, messageText);
            case "/feedback" -> handleFeedbackCommand(user.getChatId(), user);
            case "/admin" -> handleAdminCommand(user.getChatId(), user);
            default -> handleUnknownCommand(user.getChatId());
        }
    }

    private void handleExitCommand(User user) {
        if (user.getBotState().equals(BotState.WAITING_FOR_MESSAGE)) {
            sendAnswerMessage(user.getChatId(), "Вы уже находитесь в режиме ожидания.");
        } else {
            user.setBotState(BotState.WAITING_FOR_MESSAGE);
            userService.save(user);
            sendAnswerMessage(user.getChatId(), "Вы успешно вышли из текущего режима.");
        }
    }

    private void handleWeatherCommand(long chatId, User user, MessageText messageText) {
        if (user.getUserId() != null && user.getChatId() != null && user.getChatId().equals(chatId)) {
            sendAnswerMessage(chatId, "Введите локацию, для которого вы хотите узнать погоду. Команда для выхода /exit", 30);
            messageText.setMessageState(MessageState.WEATHER);
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

    public void sendAnswerMessage(long chatId, String textToSend, boolean useMarkdown) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.enableMarkdown(useMarkdown);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    public void sendAnswerMessage(long chatId, String textToSend, int delayInSeconds) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            Message sentMessage = execute(message);

            if (sentMessage != null) {
                scheduler.schedule(() -> deleteMessage(chatId, sentMessage.getMessageId()), delayInSeconds, TimeUnit.SECONDS);
            }
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void deleteMessage(long chatId, Integer messageId) {
        try {
            if (!isPrivateChat(chatId)) {
                execute(new DeleteMessage(String.valueOf(chatId), messageId));
            }
        } catch (TelegramApiException e) {
            log.error("Error occurred while deleting message: " + e.getMessage());
        }
    }

    public void deleteMessageWithTimer(long chatId, Integer messageId, int delayInSeconds) {
        if (!isPrivateChat(chatId)) {
            scheduler.schedule(() -> deleteMessage(chatId, messageId), delayInSeconds, TimeUnit.SECONDS);
        }
    }

    public MessageText checkMessage(Message messageText) {
        MessageText message = MessageText.builder()
                .chatId(messageText.getChatId())
                .userId(messageText.getFrom().getId())
                .messageId(messageText.getMessageId())
                .messageText(messageText.getText().replace("@SavvyTelegramBot", ""))
                .build();

        if (message.getMessageText().equals("/weather")) {
            message.setMessageState(MessageState.WEATHER);
        } else if (security.isPrivateChat(messageText.getChatId())) {
            message.setMessageState(MessageState.PRIVATE);
        } else {
            message.setMessageState(MessageState.DEFAULT);
        }

        if (message.getMessageState().equals(MessageState.WEATHER)) {
            deleteMessageWithTimer(message.getChatId(), message.getMessageId(), 30);
        }
        return message;
    }

    public boolean isPrivateChat(long chatId) {
        return chatId >= 0;
    }
}
