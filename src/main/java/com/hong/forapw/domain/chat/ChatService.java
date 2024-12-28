package com.hong.forapw.domain.chat;

import com.hong.forapw.domain.chat.model.MessageDetailDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.MetaDataUtils;
import com.hong.forapw.domain.chat.entity.ChatUser;
import com.hong.forapw.domain.chat.entity.LinkMetadata;
import com.hong.forapw.domain.chat.entity.Message;
import com.hong.forapw.domain.chat.constant.MessageType;
import com.hong.forapw.domain.chat.model.request.MessageDTO;
import com.hong.forapw.domain.chat.model.request.SendMessageReq;
import com.hong.forapw.domain.chat.model.response.*;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.chat.repository.ChatRoomRepository;
import com.hong.forapw.domain.chat.repository.ChatUserRepository;
import com.hong.forapw.domain.chat.repository.MessageRepository;
import com.hong.forapw.integration.rabbitmq.RabbitMqService;
import com.hong.forapw.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RabbitMqService brokerService;

    private static final String SORT_BY_MESSAGE_DATE = "date";
    private static final String URL_REGEX = "(https?://[\\w\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final Pageable DEFAULT_IMAGE_PAGEABLE = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, SORT_BY_MESSAGE_DATE));

    @Transactional
    public SendMessageRes sendMessage(SendMessageReq request, Long senderId, String senderNickName) {
        validateChatAuthorization(senderId, request.chatRoomId());

        String messageId = UUID.randomUUID().toString();
        LinkMetadata metadata = extractMetadataIfApplicable(request);
        String senderProfileURL = getUserProfileURL(senderId);

        MessageDTO messageDTO = new MessageDTO(request, senderNickName, messageId, metadata, senderProfileURL, senderId);
        publishMessageToBroker(request.chatRoomId(), messageDTO);

        return new SendMessageRes(messageId);
    }

    public FindChatRoomsRes findChatRooms(Long userId) {
        List<ChatUser> chatUsers = chatUserRepository.findAllByUserIdWithChatRoomAndGroup(userId);

        List<FindChatRoomsRes.RoomDTO> roomDTOS = chatUsers.stream()
                .map(this::buildRoomDTO)
                .toList();

        return new FindChatRoomsRes(roomDTOS);
    }

    @Transactional
    public FindMessagesInRoomRes findMessagesInRoom(Long chatRoomId, Long userId, Pageable pageable) {
        String nickName = userRepository.findNickname(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        ChatUser chatUser = chatUserRepository.findByUserIdAndChatRoomIdWithChatRoom(userId, chatRoomId).orElseThrow(
                () -> new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS)
        );

        List<Message> messages = messageRepository.findByChatRoomId(chatRoomId, pageable).getContent();
        List<FindMessagesInRoomRes.MessageDTO> messageDTOs = FindMessagesInRoomRes.MessageDTO.fromEntities(messages, userId);
        Collections.reverse(messageDTOs);

        updateLastReadMessage(chatUser, messageDTOs, chatRoomId);
        return new FindMessagesInRoomRes(chatUser.getRoomName(), chatUser.getLastReadMessageId(), nickName, messageDTOs);
    }

    public FindChatRoomDrawerRes findChatRoomDrawer(Long chatRoomId, Long userId) {
        validateChatAuthorization(userId, chatRoomId);

        List<FindChatRoomDrawerRes.ChatUserDTO> chatUserDTOs = chatRoomRepository.findUsersByChatRoomIdExcludingRole(chatRoomId, GroupRole.TEMP).stream()
                .map(FindChatRoomDrawerRes.ChatUserDTO::new)
                .toList();

        List<ImageObjectDTO> imageObjectDTOS = findImageObjects(chatRoomId, userId, DEFAULT_IMAGE_PAGEABLE).images();
        return new FindChatRoomDrawerRes(imageObjectDTOS, chatUserDTOs);
    }

    public FindImageObjectsRes findImageObjects(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatAuthorization(userId, chatRoomId);

        Page<Message> imageMessages = messageRepository.findByChatRoomIdAndMessageType(chatRoomId, MessageType.IMAGE, pageable);
        List<ImageObjectDTO> imageObjectDTOs = imageMessages.getContent().stream()
                .map(ImageObjectDTO::fromEntity)
                .toList();

        return new FindImageObjectsRes(imageObjectDTOs, imageMessages.isLast());
    }

    public FindFileObjectsRes findFileObjects(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatAuthorization(userId, chatRoomId);

        Page<Message> fileMessages = messageRepository.findByChatRoomIdAndMessageType(chatRoomId, MessageType.FILE, pageable);
        List<FindFileObjectsRes.FileObjectDTO> fileObjectDTOs = fileMessages.getContent().stream()
                .map(FindFileObjectsRes.FileObjectDTO::fromEntity)
                .toList();

        return new FindFileObjectsRes(fileObjectDTOs, fileMessages.isLast());
    }

    public FindLinkObjectsRes findLinkObjects(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatAuthorization(userId, chatRoomId);

        Page<Message> linkMessages = messageRepository.findByChatRoomIdAndMetadataOgUrlIsNotNull(chatRoomId, pageable);
        List<FindLinkObjectsRes.LinkObjectDTO> linkObjectDTOS = linkMessages.getContent().stream()
                .map(FindLinkObjectsRes.LinkObjectDTO::fromEntity)
                .toList();

        return new FindLinkObjectsRes(linkObjectDTOS, linkMessages.isLast());
    }

    @Transactional
    public ReadMessageRes readMessage(String messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new CustomException(ExceptionCode.MESSAGE_NOT_FOUND)
        );

        ChatUser chatUser = chatUserRepository.findByUserIdAndChatRoomId(message.getSenderId(), message.getChatRoomId())
                .orElseThrow(() -> new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS));

        chatUser.updateLastMessage(message.getId(), chatUser.getLastReadMessageIndex() + 1);
        return new ReadMessageRes(messageId);
    }

    private void validateChatAuthorization(Long senderId, Long chatRoomId) {
        boolean isMemberOfChatRoom = chatUserRepository.existsByUserIdAndChatRoomId(senderId, chatRoomId);
        if (!isMemberOfChatRoom) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    private String getUserProfileURL(Long senderId) {
        return userRepository.findProfileURL(senderId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
    }

    private LinkMetadata extractMetadataIfApplicable(SendMessageReq request) {
        if (request.messageType() == MessageType.TEXT) {
            String metadataURL = extractFirstURL(request.content());
            return (metadataURL != null) ? MetaDataUtils.fetchMetadata(metadataURL) : null;
        }
        return null;
    }

    private String extractFirstURL(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void publishMessageToBroker(Long chatRoomId, MessageDTO message) {
        CompletableFuture.runAsync(() -> brokerService.sendChatMessageToRoom(chatRoomId, message));
    }

    private FindChatRoomsRes.RoomDTO buildRoomDTO(ChatUser chatUser) {
        String lastMessageId = chatUser.getLastReadMessageId();
        MessageDetailDTO lastMessageDetails = fetchLastMessageContentAndDate(lastMessageId);

        Long unreadMessageIndex = chatUser.getLastReadMessageIndex();
        long unreadMessageOffset = calculateUnreadMessageOffset(chatUser.getChatRoom().getId(), unreadMessageIndex);

        return new FindChatRoomsRes.RoomDTO(chatUser, lastMessageDetails, unreadMessageOffset);
    }

    private MessageDetailDTO fetchLastMessageContentAndDate(String lastMessageId) {
        if (lastMessageId == null) {
            return new MessageDetailDTO(null, null);
        }

        return messageRepository.findById(lastMessageId)
                .map(message -> new MessageDetailDTO(message.getContent(), message.getDate()))
                .orElse(new MessageDetailDTO(null, null));
    }

    private long calculateUnreadMessageOffset(Long chatRoomId, Long lastReadMessageIdx) {
        long totalMessages = messageRepository.countByChatRoomId(chatRoomId);
        long totalPages = totalMessages != 0L ? (totalMessages / 50) : 0L;

        if (lastReadMessageIdx == null || lastReadMessageIdx == 0L) {
            return totalPages;
        }

        long lastReadPage = lastReadMessageIdx / 50;
        return totalPages - lastReadPage;
    }

    private void updateLastReadMessage(ChatUser chatUser, List<FindMessagesInRoomRes.MessageDTO> messageDTOs, Long chatRoomId) {
        if (!messageDTOs.isEmpty()) {
            FindMessagesInRoomRes.MessageDTO lastMessage = messageDTOs.get(messageDTOs.size() - 1);
            long totalMessages = messageRepository.countByChatRoomId(chatRoomId);
            chatUser.updateLastMessage(lastMessage.messageId(), totalMessages - 1);
        }
    }
}