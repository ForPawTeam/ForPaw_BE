package com.hong.forapw.domain.chat;

import com.hong.forapw.domain.chat.model.ChatResponse;
import com.hong.forapw.domain.chat.model.request.SendMessageReq;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private static final String SORT_BY_DATE = "date";
    private static final String SORT_BY_ID = "id";

    @PostMapping("/chat/send")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.SendMessageDTO responseDTO = chatService.sendMessage(request, userDetails.user().getId(), userDetails.getUsername());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms")
    public ResponseEntity<?> findChatRoomList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.FindChatRoomsDTO responseDTO = chatService.findChatRooms(userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/messages")
    public ResponseEntity<?> findMessageListInRoom(@PathVariable Long chatRoomId,
                                                   @PageableDefault(size = 50, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.FindMessagesInRoomDTO responseDTO = chatService.findMessagesInRoom(chatRoomId, userDetails.user().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/drawer")
    public ResponseEntity<?> findChatRoomDrawer(@PathVariable Long chatRoomId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.FindChatRoomDrawerDTO responseDTO = chatService.findChatRoomDrawer(chatRoomId, userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/images")
    public ResponseEntity<?> findImageObjectList(@PathVariable Long chatRoomId,
                                                 @PageableDefault(size = 6, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.FindImageObjectsDTO responseDTO = chatService.findImageObjects(chatRoomId, userDetails.user().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/files")
    public ResponseEntity<?> findFileObjectList(@PathVariable Long chatRoomId,
                                                @PageableDefault(size = 6, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.FindFileObjects responseDTO = chatService.findFileObjects(chatRoomId, userDetails.user().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/links")
    public ResponseEntity<?> findLinkObjectList(@PathVariable Long chatRoomId,
                                                @PageableDefault(size = 6, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.FindLinkObjects responseDTO = chatService.findLinkObjects(chatRoomId, userDetails.user().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/chat/{chatId}/read")
    public ResponseEntity<?> readMessage(@PathVariable String chatId) {
        ChatResponse.ReadMessageDTO responseDTO = chatService.readMessage(chatId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
