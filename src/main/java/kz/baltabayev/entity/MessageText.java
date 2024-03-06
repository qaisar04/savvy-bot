package kz.baltabayev.entity;

import kz.baltabayev.entity.type.MessageState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageText {
    private String messageText;
    private Integer messageId;
    private Long chatId;
    private Long userId;
    private MessageState messageState;
}
