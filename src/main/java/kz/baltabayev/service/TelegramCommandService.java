package kz.baltabayev.service;

import kz.baltabayev.controller.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramCommandService {

    public void showHelpCommandReceived(long chatId, TelegramBot bot) {
        String helpMessage = "Список доступных команд:\n" +
                             "/coinflip - подбросить монетку\n" +
                             "/weather - узнать информацию о погоде\n" +
                             "/help - показать список доступных команд\n" +
                             "/feedback - оставить обратную связь о боте\n\n" +
                             "группа в телеграмме: @prgrm_java";

        bot.sendAnswerMessage(chatId, helpMessage);
    }

    public void coinFlipCommandReceived(long chatId, TelegramBot bot) {
        String[] coinSides = {"орёл 🦅", "решка 💰"};
        int randomIndex = ThreadLocalRandom.current().nextInt(coinSides.length);
        String result = coinSides[randomIndex];

        String answer = String.format("Монетка была подброшена... и она выпала на: %s", result);
        bot.sendAnswerMessage(chatId, answer);
    }

    public void startCommandReceived(long chatId, String username, TelegramBot bot) {
        String answer = "Привет, " + username + ". Рад видеть вас!\n" +
                        "/help - Показать список доступных команд";
        log.info("Replide to user " + username);
        bot.sendAnswerMessage(chatId, answer);
    }




}
