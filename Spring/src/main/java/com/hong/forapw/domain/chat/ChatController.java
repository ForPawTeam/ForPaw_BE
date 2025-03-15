package com.hong.forapw.domain.chat;

import com.hong.forapw.domain.chat.model.request.SendMessageReq;
import com.hong.forapw.domain.chat.model.response.*;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_MESSAGE_DATE;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat/send")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        SendMessageRes response = chatService.sendChat(request, userDetails.getUserId(), userDetails.getUsername());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/chatRooms")
    public ResponseEntity<?> findChatRooms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindChatRoomsRes response = chatService.findChatRooms(userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/chatRooms/{chatRoomId}/messages")
    public ResponseEntity<?> findMessageListInRoom(@PathVariable Long chatRoomId,
                                                   @PageableDefault(size = 50, sort = SORT_BY_MESSAGE_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindMessagesInRoomRes response = chatService.findChatInRoom(chatRoomId, userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/chatRooms/{chatRoomId}/drawer")
    public ResponseEntity<?> findChatRoomDrawer(@PathVariable Long chatRoomId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindChatRoomDrawerRes response = chatService.findChatRoomDrawer(chatRoomId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/chatRooms/{chatRoomId}/images")
    public ResponseEntity<?> findImageObjects(@PathVariable Long chatRoomId,
                                              @PageableDefault(size = 6, sort = SORT_BY_MESSAGE_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindImageObjectsRes response = chatService.findImageObjects(chatRoomId, userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/chatRooms/{chatRoomId}/files")
    public ResponseEntity<?> findFileObjects(@PathVariable Long chatRoomId,
                                             @PageableDefault(size = 6, sort = SORT_BY_MESSAGE_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindFileObjectsRes response = chatService.findFileObjects(chatRoomId, userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/chatRooms/{chatRoomId}/links")
    public ResponseEntity<?> findLinkObjects(@PathVariable Long chatRoomId,
                                             @PageableDefault(size = 6, sort = SORT_BY_MESSAGE_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindLinkObjectsRes response = chatService.findLinkObjects(chatRoomId, userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/chat/{chatId}/read")
    public ResponseEntity<?> readMessage(@PathVariable String chatId) {
        ReadMessageRes response = chatService.readChat(chatId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}
