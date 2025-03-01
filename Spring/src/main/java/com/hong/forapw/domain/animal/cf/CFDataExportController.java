package com.hong.forapw.domain.animal.cf;

import com.hong.forapw.common.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class CFDataExportController {

    private final CFDataExportService cfDataExportService;

    @GetMapping("/interactions")
    public ResponseEntity<?> exportInteractions(){
        cfDataExportService.exportInteractionsToFastAPI();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}
