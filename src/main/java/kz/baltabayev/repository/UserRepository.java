package kz.baltabayev.repository;

import kz.baltabayev.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByChatId(Long chatId);

    Optional<User> findByUserId(Long userId);

    Optional<User> findByUserIdAndChatId(Long userId, Long chatId);

    @Query("SELECT DISTINCT u.userId, u.chatId FROM User u")
    List<Long> findAllDistinctUserIdAndChatId();
}
