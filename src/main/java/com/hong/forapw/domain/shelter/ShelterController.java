package com.hong.forapw.domain.shelter;

import com.hong.forapw.domain.shelter.model.response.FindShelterAnimalsByIdRes;
import com.hong.forapw.domain.shelter.model.response.FindShelterInfoByIdRes;
import com.hong.forapw.domain.shelter.model.response.FindShelterListRes;
import com.hong.forapw.domain.shelter.model.response.FindShelterListWithAddrRes;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_DATE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ShelterController {

    private final ShelterService shelterService;

    // 테스트 시에만 API를 열어둠
    @GetMapping("/shelters/import")
    public ResponseEntity<?> loadShelter(@AuthenticationPrincipal CustomUserDetails userDetails) {
        shelterService.updateNewShelters();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/shelters")
    public ResponseEntity<?> findShelters() {
        FindShelterListRes response = shelterService.findActiveShelters();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, response));
    }

    @GetMapping("/shelters/{shelterId}/info")
    public ResponseEntity<?> findShelterInfoById(@PathVariable Long shelterId) {
        FindShelterInfoByIdRes response = shelterService.findShelterInfoById(shelterId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/shelters/{shelterId}/animals")
    public ResponseEntity<?> findShelterAnimalsById(@PathVariable Long shelterId, @RequestParam String type,
                                                    @PageableDefault(size = 5, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindShelterAnimalsByIdRes response = shelterService.findAnimalsByShelter(shelterId, userDetails.getUserIdOrNull(), type, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/shelters/addr")
    public ResponseEntity<?> findShelterListWithAddr() {
        FindShelterListWithAddrRes response = shelterService.findSheltersWithAddress();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}