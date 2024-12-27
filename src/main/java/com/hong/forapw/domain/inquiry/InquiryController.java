package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.inquiry.model.InquriyRequest;
import com.hong.forapw.domain.inquiry.model.response.FindInquiryListRes;
import com.hong.forapw.domain.inquiry.model.response.SubmitInquiryRes;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/supports")
    public ResponseEntity<?> submitInquiry(@RequestBody @Valid InquriyRequest.SubmitInquiry requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubmitInquiryRes response = inquiryService.submitInquiry(requestDTO, userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/supports/{inquiryId}")
    public ResponseEntity<?> updateInquiry(@RequestBody @Valid InquriyRequest.UpdateInquiry requestDTO, @PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        inquiryService.updateInquiry(requestDTO, inquiryId, userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/supports")
    public ResponseEntity<?> findInquiryList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindInquiryListRes response = inquiryService.findInquiries(userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}
