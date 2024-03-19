package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, Long> {

    Page<Message> findByChatRoomId(Long chatRoomId, Pageable pageable);

    // 특정 시간 이후의 채팅
    Page<Message> findByChatRoomIdAndDateGreaterThan(Long chatRoomId, LocalDateTime date, Pageable pageable);
}
