package kz.baltabayev.service;

import kz.baltabayev.entity.User;
import kz.baltabayev.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    public Optional<User> getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public Optional<User> getUserByUserIdAndChatId(Long userId, Long chatId) {
        return userRepository.findByUserIdAndChatId(userId, chatId);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public Optional<User> findByUserId(Long userId) {
        return userRepository.findByUserId(userId);
    }

    public List<Long> getAllDistinctUserIdAndChatId() {
        return userRepository.findAllDistinctUserIdAndChatId();
    }
}

