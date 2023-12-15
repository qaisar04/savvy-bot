package kz.baltabayev.service;

import kz.baltabayev.controller.TelegramBot;
import kz.baltabayev.entity.Security;
import kz.baltabayev.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final SecurityRepository securityRepository;

    public void sendConfirmationCode(TelegramBot bot) {
        String uuidCode = generateUUID();
        Security build = Security.builder()
                .createdAt(LocalDateTime.now())
                .uuidCode(uuidCode)
                .build();
        bot.sendAnswerMessage(697119914, uuidCode);
        securityRepository.save(build);
    }

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public boolean isPrivateChat(long chatId, TelegramBot bot) {
        if (chatId < 0) {
            bot.sendAnswerMessage(chatId, "Пожалуйста, применяйте указанную команду в личных сообщениях.");
            return false;
        }
        return true;
    }

    public Optional<Security> findByUuidCode(String code) {
        return securityRepository.findByUuidCode(code);
    }

    public void delete(Security security) {
        securityRepository.delete(security);
    }

}
