package com.hong.forapw.domain.home;

import com.hong.forapw.domain.home.model.response.FindHomeRes;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.hong.forapw.security.userdetails.CustomUserDetails.getUserIdOrNull;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public ResponseEntity<?> findHome(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindHomeRes response = homeService.findHomePageData(getUserIdOrNull(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}