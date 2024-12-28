package com.hong.forapw.domain.apply;

import com.hong.forapw.domain.apply.model.ApplyResponse;
import com.hong.forapw.domain.apply.model.request.ApplyAdoptionReq;
import com.hong.forapw.domain.apply.model.request.UpdateApplyReq;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApplyController {

    private final ApplyService applyService;

    @PostMapping("/animals/{animalId}/apply")
    public ResponseEntity<?> applyAdoption(@RequestBody @Valid ApplyAdoptionReq request, @PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplyResponse.CreateApplyDTO responseDTO = applyService.applyAdoption(request, userDetails.user().getId(), animalId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/applies")
    public ResponseEntity<?> findApplyList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplyResponse.FindApplyListDTO response = applyService.findApplyList(userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/applies/{applyId}")
    public ResponseEntity<?> updateApply(@RequestBody @Valid UpdateApplyReq request, @PathVariable Long applyId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        applyService.updateApply(request, applyId, userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    // 권한 처리가 필요함
    @DeleteMapping("/applies/{applyId}")
    public ResponseEntity<?> deleteApply(@PathVariable Long applyId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        applyService.deleteApply(applyId, userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}
