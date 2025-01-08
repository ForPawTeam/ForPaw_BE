package com.hong.forapw.domain.animal;

import com.hong.forapw.domain.animal.model.response.FindAnimalByIdRes;
import com.hong.forapw.domain.animal.model.response.FindAnimalListRes;
import com.hong.forapw.domain.animal.model.response.FindLikeAnimalListRes;
import com.hong.forapw.domain.animal.model.response.FindRecommendedAnimalListRes;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_DATE;
import static com.hong.forapw.security.userdetails.CustomUserDetails.getUserIdOrNull;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AnimalController {

    private final AnimalService animalService;
    private final LikeService likeService;

    // 테스트시에만 열어둠
    @GetMapping("/animals/import")
    public ResponseEntity<?> loadAnimals() {
        animalService.updateNewAnimals();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/animals/recommend")
    public ResponseEntity<?> findRecommendedAnimalList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindRecommendedAnimalListRes response = animalService.findRecommendedAnimals(getUserIdOrNull(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/animals")
    public ResponseEntity<?> findAnimalList(@RequestParam String type,
                                            @PageableDefault(size = 5, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindAnimalListRes response = animalService.findAnimals(type, getUserIdOrNull(userDetails), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/animals/like")
    public ResponseEntity<?> findLikeAnimalList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindLikeAnimalListRes response = animalService.findLikeAnimals(userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/animals/{animalId}")
    public ResponseEntity<?> findAnimalById(@PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindAnimalByIdRes response = animalService.findAnimalById(animalId, getUserIdOrNull(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/animals/{animalId}/like")
    public ResponseEntity<?> likeAnimal(@PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        likeService.like(animalId, userDetails.getUserId(), Like.ANIMAL);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}