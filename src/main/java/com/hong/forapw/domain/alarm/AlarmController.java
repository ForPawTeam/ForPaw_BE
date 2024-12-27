package com.hong.forapw.domain.alarm;

import com.hong.forapw.domain.alarm.model.request.ReadAlarmReq;
import com.hong.forapw.domain.alarm.model.response.FindAlarmListRes;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping(value = "/alarms/connect", produces = "text/event-stream")
    public SseEmitter connectToAlarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return alarmService.connectToSseForAlarms(userDetails.user().getId().toString());
    }

    @GetMapping("/alarms")
    public ResponseEntity<?> findAlarmList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindAlarmListRes responseDTO = alarmService.findAlarms(userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/alarms/read")
    public ResponseEntity<?> readAlarm(@RequestBody @Valid ReadAlarmReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        alarmService.updateAlarmAsRead(request.id(), userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}