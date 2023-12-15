package kz.baltabayev.repository;

import kz.baltabayev.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByChatId(Long chatId);

    Optional<User> findByUserId(Long userId);

}
