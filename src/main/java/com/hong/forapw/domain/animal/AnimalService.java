package com.hong.forapw.domain.animal;

import com.hong.forapw.domain.animal.model.PublicAnimalDTO;
import com.hong.forapw.domain.animal.model.AnimalJsonDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.JsonParser;
import com.hong.forapw.domain.animal.constant.AnimalType;
import com.hong.forapw.domain.animal.model.RecommendationDTO;
import com.hong.forapw.domain.animal.model.response.*;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.shelter.Shelter;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.domain.shelter.ShelterRepository;
import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.domain.shelter.ShelterService;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.like.LikeService;
import jakarta.persistence.EntityManager;
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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hong.forapw.common.constants.GlobalConstants.ANIMAL_SEARCH_KEY;
import static com.hong.forapw.common.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.common.utils.PaginationUtils.DEFAULT_PAGEABLE;
import static com.hong.forapw.common.utils.PaginationUtils.isLastPage;
import static com.hong.forapw.common.utils.UriUtils.buildAnimalOpenApiURI;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final LikeService likeService;
    private final ShelterService shelterService;
    private final EntityManager entityManager;
    private final WebClient webClient;
    private final JsonParser jsonParser;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${animal.names}")
    private String[] animalNames;

    @Value("${recommend.uri}")
    private String animalRecommendURI;

    @Value("${animal.update.uri}")
    private String updateAnimalIntroduceURI;

    @Value("${openAPI.animal.uri}")
    private String animalURI;

    private static final int MAX_RECOMMENDED_ANIMALS = 5;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void updateNewAnimals() {
        List<Long> existingAnimalIds = animalRepository.findAllIds();
        
        List<Shelter> shelters = shelterRepository.findAllWithRegionCode();
        List<AnimalJsonDTO> animalJsonDTO = fetchAnimalDataFromApi(shelters);
        
        saveNewAnimalData(animalJsonDTO, existingAnimalIds);

        shelterService.updateShelter(animalJsonDTO);
        postProcessAfterAnimalUpdate();
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

        return new FindAnimalByIdRes(animal,isLikedAnimal);
    }

    // 로그인 X => 그냥 최신순, 로그인 O => 검색 기록을 바탕으로 추천 => 검색 기록이 없다면 위치를 기준으로 주변 보호소의 동물 추천
    public List<Long> findRecommendedAnimalIds(Long userId) {
        if (userId == null) {
            return getLatestAnimalIds();
        }

        List<Long> recommendedAnimalIds = fetchRecommendedAnimalIds(userId);
        if (recommendedAnimalIds.isEmpty()) {
            recommendedAnimalIds = getAnimalIdsByUserLocation(userId);
        }

        return recommendedAnimalIds;
    }

    private List<AnimalJsonDTO> fetchAnimalDataFromApi(List<Shelter> shelters) {
        return Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(75))
                .flatMap(shelter -> buildAnimalOpenApiURI(animalURI, serviceKey, shelter.getId())
                        .flatMap(uri -> webClient.get()
                                .uri(uri)
                                .retrieve()
                                .bodyToMono(String.class)
                                .retry(3)
                                .map(rawJsonResponse -> new AnimalJsonDTO(shelter, rawJsonResponse)))
                        .onErrorResume(e -> {
                            log.error("Shelter {} 데이터 가져오기 실패: {}", shelter.getId(), e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .block();
    }

    public void saveNewAnimalData(List<AnimalJsonDTO> animalJsonResponse, List<Long> existingAnimalIds) {
        for (AnimalJsonDTO response : animalJsonResponse) {
            Shelter shelter = response.shelter();
            String animalJson = response.animalJson();

            List<Animal> animals = convertAnimalJsonToAnimals(animalJson, shelter, existingAnimalIds);
            animalRepository.saveAll(animals);
        }
        entityManager.flush();
    }

    private List<Animal> convertAnimalJsonToAnimals(String animalJson, Shelter shelter, List<Long> existingAnimalIds) {
        return jsonParser.parse(animalJson, PublicAnimalDTO.class)
                .map(PublicAnimalDTO::response)
                .map(PublicAnimalDTO.ResponseDTO::body)
                .map(PublicAnimalDTO.BodyDTO::items)
                .map(PublicAnimalDTO.ItemsDTO::item)
                .orElse(Collections.emptyList())
                .stream()
                .filter(animalDTO -> isNewAnimal(animalDTO, existingAnimalIds))
                .filter(this::isAdoptionNoticeValid)
                .map(animalDTO -> {
                    String name = createAnimalName();
                    String kind = parseSpecies(animalDTO.kindCd());
                    return animalDTO.toEntity(name, kind, shelter);
                })
                .toList();
    }

    private boolean isNewAnimal(PublicAnimalDTO.AnimalDTO item, List<Long> existingAnimalIds) {
        return !existingAnimalIds.contains(Long.valueOf(item.desertionNo()));
    }

    private boolean isAdoptionNoticeValid(PublicAnimalDTO.AnimalDTO item) {
        return LocalDate.parse(item.noticeEdt(), YEAR_HOUR_DAY_FORMAT).isAfter(LocalDate.now());
    }

    private String createAnimalName() {
        int index = ThreadLocalRandom.current().nextInt(animalNames.length);
        return animalNames[index];
    }

    private String parseSpecies(String input) {
        Pattern pattern = Pattern.compile("\\[.*?\\] (.+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void postProcessAfterAnimalUpdate() {
        List<Animal> expiredAnimals = handleExpiredAnimals();

        Set<Shelter> updatedShelters = getUpdatedShelters(expiredAnimals);
        updateShelterAnimalCounts(updatedShelters);

        updateAnimalIntroductions();
        resolveDuplicateShelters();
    }

    private List<Animal> handleExpiredAnimals() {
        List<Animal> expiredAnimals = animalRepository.findAllOutOfDateWithShelter(LocalDateTime.now().toLocalDate());

        removeAnimalLikesFromCache(expiredAnimals);
        removeAnimalsFromUserSearchHistory(expiredAnimals);
        favoriteAnimalRepository.deleteByAnimalIn(expiredAnimals);
        animalRepository.deleteAll(expiredAnimals);

        return expiredAnimals;
    }

    private void removeAnimalLikesFromCache(List<Animal> expiredAnimals) {
        expiredAnimals.forEach(animal -> likeService.clearAnimalLikeData(animal.getId()));
    }

    private void removeAnimalsFromUserSearchHistory(List<Animal> expiredAnimals) {
        List<User> users = userRepository.findAllNonWithdrawn();
        for (User user : users) {
            String key = ANIMAL_SEARCH_KEY + ":" + user.getId();
            expiredAnimals.forEach(animal ->
                    redisService.removeListElement(key, animal.getId().toString())
            );
        }
    }

    private void updateShelterAnimalCounts(Set<Shelter> updatedShelters) {
        updatedShelters.forEach(shelter ->
                shelter.updateAnimalCount(animalRepository.countByShelterId(shelter.getId()))
        );
    }

    private Set<Shelter> getUpdatedShelters(List<Animal> expiredAnimals) {
        return expiredAnimals.stream()
                .map(Animal::getShelter)
                .collect(Collectors.toSet());
    }

    private void updateAnimalIntroductions() {
        webClient.post()
                .uri(updateAnimalIntroduceURI)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::mapError)
                .bodyToMono(Void.class)
                .retryWhen(createRetrySpec())
                .onErrorResume(error -> {
                    log.error("소개글 업데이트 중 에러 발생: {}", error.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    private Mono<Throwable> mapError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> Mono.empty());
    }

    private Retry createRetrySpec() {
        return Retry.fixedDelay(3, Duration.ofSeconds(2))
                .filter(CustomException.class::isInstance)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        new CustomException(ExceptionCode.INTRODUCTION_UPDATE_FAILED)
                );
    }

    private void resolveDuplicateShelters() {
        List<String> duplicateCareTels = shelterRepository.findDuplicateCareTels();
        duplicateCareTels.forEach(this::handleDuplicateSheltersForCareTel);
    }

    private void handleDuplicateSheltersForCareTel(String careTel) {
        List<Shelter> shelters = shelterRepository.findByCareTel(careTel);
        Shelter targetShelter = getTargetShelter(shelters);
        List<Long> duplicateShelterIds = getDuplicateShelterIds(shelters, targetShelter);

        if (!duplicateShelterIds.isEmpty()) {
            animalRepository.updateShelterByShelterIds(targetShelter, duplicateShelterIds);
            shelterRepository.updateIsDuplicateByIds(duplicateShelterIds, true);
        }

        shelterRepository.updateIsDuplicate(targetShelter.getId(), false);
    }

    private Shelter getTargetShelter(List<Shelter> shelters) {
        return shelters.stream()
                .filter(shelter -> !shelter.isDuplicate())
                .min(Comparator.comparingLong(Shelter::getId))
                .orElse(shelters.get(0));
    }

    private List<Long> getDuplicateShelterIds(List<Shelter> shelters, Shelter targetShelter) {
        return shelters.stream()
                .filter(shelter -> !shelter.equals(targetShelter))
                .map(Shelter::getId)
                .toList();
    }

    private void saveSearchRecord(Long animalId, Long userId) {
        if (userId != null) {
            String key = ANIMAL_SEARCH_KEY + ":" + userId;
            redisService.addListElement(key, animalId.toString(), 5L);
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