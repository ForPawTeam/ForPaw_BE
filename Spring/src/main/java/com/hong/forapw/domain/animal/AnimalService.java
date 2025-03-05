package com.hong.forapw.domain.animal;

import com.hong.forapw.domain.animal.cf.CFDataExportService;
import com.hong.forapw.domain.animal.model.AnimalJsonDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.constant.AnimalType;
import com.hong.forapw.domain.animal.model.RecommendationDTO;
import com.hong.forapw.domain.animal.model.response.*;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.shelter.Shelter;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.domain.shelter.ShelterRepository;
import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.domain.shelter.ShelterService;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.hong.forapw.common.constants.GlobalConstants.ANIMAL_SEARCH_KEY;
import static com.hong.forapw.common.utils.PaginationUtils.DEFAULT_PAGEABLE;
import static com.hong.forapw.common.utils.PaginationUtils.isLastPage;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;
    private final CFDataExportService userAnimalInteractionService;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final LikeService likeService;
    private final ShelterService shelterService;
    private final AnimalDataSaveService animalDataSaveService;
    private final WebClient webClient;

    @Value("${recommend.uri}")
    private String animalRecommendURI;

    private static final int MAX_RECOMMENDED_ANIMALS = 5;

    @Scheduled(cron = "0 0 0 * * *")
    public void updateNewAnimals() {
        List<Long> existingAnimalIds = animalRepository.findAllIds();

        List<Shelter> shelters = shelterRepository.findAllWithRegionCode();
        List<AnimalJsonDTO> animalJsonDTO = animalDataSaveService.fetchAnimalDataFromApi(shelters);

        animalDataSaveService.saveNewAnimalData(animalJsonDTO, existingAnimalIds);

        shelterService.updateShelter(animalJsonDTO);
        animalDataSaveService.postProcessAfterAnimalUpdate();
    }

    public FindAnimalListRes findAnimals(String type, Long userId, Pageable pageable) {
        Page<Animal> animalPage = animalRepository.findByAnimalType(AnimalType.fromString(type), pageable);
        List<Long> animalIds = animalPage.getContent().stream()
                .map(Animal::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(animalIds, Like.ANIMAL);
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();
        List<AnimalDTO> animalDTOs = AnimalDTO.fromEntities(animalPage.getContent(), likeCountMap, likedAnimalIds);

        return new FindAnimalListRes(animalDTOs, isLastPage(animalPage));
    }

    public FindRecommendedAnimalListRes findRecommendedAnimals(Long userId) {
        List<Long> recommendedAnimalIds = findRecommendedAnimalIds(userId);

        List<Animal> animals = animalRepository.findByIds(recommendedAnimalIds);
        List<Long> animalIds = animals.stream()
                .map(Animal::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(animalIds, Like.ANIMAL);
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();
        List<AnimalDTO> animalDTOs = AnimalDTO.fromEntities(animals, likeCountMap, likedAnimalIds);

        return new FindRecommendedAnimalListRes(animalDTOs);
    }

    public FindLikeAnimalListRes findLikeAnimals(Long userId) {
        List<Animal> animals = favoriteAnimalRepository.findAnimalsByUserId(userId);
        List<Long> animalIds = animals.stream()
                .map(Animal::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(animalIds, Like.ANIMAL);
        List<AnimalDTO> animalDTOs = AnimalDTO.fromEntities(animals, likeCountMap, Collections.emptyList());

        return new FindLikeAnimalListRes(animalDTOs);
    }

    public FindAnimalByIdRes findAnimalById(Long animalId, Long userId) {
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        boolean isLikedAnimal = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animal.getId()).isPresent();
        saveSearchRecord(animalId, userId);

        return new FindAnimalByIdRes(animal, isLikedAnimal);
    }

    // 로그인 X => 그냥 최신순, 로그인 O => 검색 기록을 바탕으로 추천 => 검색 기록이 없다면 위치를 기준으로 주변 보호소의 동물 추천
    public List<Long> findRecommendedAnimalIds(Long userId) {
        if (userId == null) {
            return getLatestAnimalIds();
        }

        List<Long> recommendedAnimalIds = fetchRecommendedAnimalIds(userId);
        if (recommendedAnimalIds == null || recommendedAnimalIds.isEmpty()) {
            recommendedAnimalIds = getAnimalIdsByUserLocation(userId);
        }

        return recommendedAnimalIds;
    }

    private void saveSearchRecord(Long animalId, Long userId) {
        if (userId != null) {
            String key = ANIMAL_SEARCH_KEY + ":" + userId;
            redisService.addListElement(key, animalId.toString(), 5L);

            userAnimalInteractionService.updateInteraction(userId, animalId, 0, 1, 0);
        }
    }

    private List<Long> getLatestAnimalIds() {
        return animalRepository.findAllIds(DEFAULT_PAGEABLE).getContent();
    }

    private List<Long> fetchRecommendedAnimalIds(Long userId) {
        Map<String, Long> requestBody = Map.of("user_id", userId);
        return webClient.post()
                .uri(animalRecommendURI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(RecommendationDTO.class)
                .map(RecommendationDTO::recommendedAnimals)
                .onErrorResume(e -> {
                    log.warn("FastAPI 호출 시 에러 발생: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    private List<Long> getAnimalIdsByUserLocation(Long userId) {
        List<Long> animalIds = getAnimalIdsByDistrict(userId);
        if (animalIds.size() < MAX_RECOMMENDED_ANIMALS) {
            addAnimalIdsFromProvince(userId, animalIds);
        }

        return limitToMaxSize(animalIds);
    }

    private List<Long> getAnimalIdsByDistrict(Long userId) {
        return userRepository.findDistrictById(userId)
                .map(district -> animalRepository.findIdsByDistrict(district, DEFAULT_PAGEABLE))
                .orElseGet(ArrayList::new);
    }

    private void addAnimalIdsFromProvince(Long userId, List<Long> animalIds) {
        userRepository.findProvinceById(userId).ifPresent(province -> {
            List<Long> provinceAnimalIds = animalRepository.findIdsByProvince(province, DEFAULT_PAGEABLE);
            animalIds.addAll(provinceAnimalIds);
        });
    }

    private List<Long> limitToMaxSize(List<Long> animalIds) {
        return animalIds.size() > MAX_RECOMMENDED_ANIMALS ? animalIds.subList(0, MAX_RECOMMENDED_ANIMALS) : animalIds;
    }
}