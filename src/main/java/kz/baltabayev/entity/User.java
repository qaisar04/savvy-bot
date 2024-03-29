package kz.baltabayev.entity;

import jakarta.persistence.*;
import kz.baltabayev.entity.type.BotState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users", schema = "dev")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_state", columnDefinition = "VARCHAR(128) default 'WAITING_FOR_MESSAGE'")
    private BotState botState;

}

