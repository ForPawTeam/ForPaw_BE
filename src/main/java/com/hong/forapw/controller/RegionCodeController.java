package com.hong.forapw.controller;

import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.service.RegionCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RegionCodeController {

    private final RegionCodeService regionCodeService;

    @GetMapping("/regionCodes/import")
    public ResponseEntity<?> loadRegionCode() throws IOException {
        regionCodeService.updateRegionCodeData();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}