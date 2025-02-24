package com.hong.forapw.domain.shelter;

import com.hong.forapw.domain.animal.model.PublicAnimalDTO;
import com.hong.forapw.domain.animal.model.AnimalJsonDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.JsonParser;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.animal.constant.AnimalType;
import com.hong.forapw.domain.like.LikeService;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.region.entity.RegionCode;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.domain.region.RegionCodeRepository;
import com.hong.forapw.domain.shelter.model.response.FindShelterAnimalsByIdRes;
import com.hong.forapw.domain.shelter.model.response.FindShelterInfoByIdRes;
import com.hong.forapw.domain.shelter.model.response.FindShelterListRes;
import com.hong.forapw.domain.shelter.model.response.FindShelterListWithAddrRes;
import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.integration.geocoding.model.Coordinates;
import com.hong.forapw.integration.geocoding.service.GeocodingService;
import com.hong.forapw.integration.geocoding.service.GoogleGeocodingService;
import com.hong.forapw.domain.shelter.model.PublicShelterDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.common.constants.GlobalConstants.ANIMAL_LIKE_NUM_KEY;
import static com.hong.forapw.common.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.common.utils.PaginationUtils.isLastPage;
import static com.hong.forapw.common.utils.UriUtils.buildShelterOpenApiURI;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final AnimalRepository animalRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final WebClient webClient;
    private final JsonParser jsonParser;
    private final GoogleGeocodingService googleService;
    private final LikeService likeService;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.shelter.uri}")
    private String baseUrl;

    private static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달

    @Transactional
    @Scheduled(cron = "0 0 6 * * MON")
    public void updateNewShelters() {
        List<Long> savedShelterIds = shelterRepository.findAllIds(); // 이미 저장되어 있는 보호소는 배제
        List<RegionCode> regionCodes = regionCodeRepository.findAll();

        Flux.fromIterable(regionCodes)
                .delayElements(Duration.ofMillis(50))
                .flatMap(regionCode -> fetchShelterDataFromApi(regionCode, savedShelterIds))
                .collectList()
                .doOnNext(shelterRepository::saveAll)
                .doOnError(error -> log.error("보호소 데이터 패치 실패: {}", error.getMessage()))
                .subscribe();
    }

    @Transactional
    public void updateShelter(List<AnimalJsonDTO> animalJsonResponse) {
        for (AnimalJsonDTO response : animalJsonResponse) {
            Shelter shelter = response.shelter();
            String animalJson = response.animalJson();
            updateShelterByAnimalData(animalJson, shelter);
        }

        updateShelterAddressByGoogle();
    }

    public FindShelterListRes findActiveShelters() {
        List<Shelter> shelters = shelterRepository.findAllWithAnimalAndLatitude();

        List<FindShelterListRes.ShelterDTO> shelterDTOS = shelters.stream()
                .map(FindShelterListRes.ShelterDTO::fromEntity)
                .toList();

        return new FindShelterListRes(shelterDTOS);
    }

    public FindShelterInfoByIdRes findShelterInfoById(Long shelterId) {
        Shelter shelter = shelterRepository.findById(shelterId).orElseThrow(
                () -> new CustomException(ExceptionCode.SHELTER_NOT_FOUND)
        );

        return FindShelterInfoByIdRes.fromEntity(shelter);
    }

    public FindShelterAnimalsByIdRes findAnimalsByShelter(Long shelterId, Long userId, String type, Pageable pageable) {
        Page<Animal> animalPage = animalRepository.findByShelterIdAndType(AnimalType.fromString(type), shelterId, pageable);
        List<Long> animalIds = animalPage.getContent().stream()
                .map(Animal::getId)
                .toList();

        Map<Long, Long> likeCounts = likeService.getLikeCounts(animalIds, Like.ANIMAL);
        List<Long> userLikedAnimalIds = getUserLikedAnimalIds(userId);

        List<FindShelterAnimalsByIdRes.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long likeNum = likeCounts.get(animal.getId());
                    boolean isLikedAnimal = userLikedAnimalIds.contains(animal.getId());
                    return FindShelterAnimalsByIdRes.AnimalDTO.fromEntity(animal, isLikedAnimal, likeNum);
                })
                .toList();

        return new FindShelterAnimalsByIdRes(animalDTOS, isLastPage(animalPage));
    }

    public FindShelterListWithAddrRes findSheltersWithAddress() {
        List<Shelter> shelters = shelterRepository.findAll();
        return FindShelterListWithAddrRes.fromShelters(shelters);
    }

    private void updateShelterByAnimalData(String animalJson, Shelter shelter) {
        jsonParser.parse(animalJson, PublicAnimalDTO.class).ifPresent(
                animalDTO -> updateShelterWithAnimalData(animalDTO, shelter)
        );
    }

    private void updateShelterWithAnimalData(PublicAnimalDTO animalData, Shelter shelter) {
        getFirstAnimalItem(animalData)
                .ifPresent(firstAnimalItem -> shelterRepository.updateShelterInfo(
                        firstAnimalItem.careTel(), firstAnimalItem.careAddr(), countActiveAnimals(animalData), shelter.getId())
                );
    }

    private Optional<PublicAnimalDTO.AnimalDTO> getFirstAnimalItem(PublicAnimalDTO animalDTO) {
        return Optional.ofNullable(animalDTO)
                .map(PublicAnimalDTO::response)
                .map(PublicAnimalDTO.ResponseDTO::body)
                .map(PublicAnimalDTO.BodyDTO::items)
                .map(PublicAnimalDTO.ItemsDTO::item)
                .filter(items -> !items.isEmpty())
                .map(items -> items.get(0));
    }

    private long countActiveAnimals(PublicAnimalDTO animalDTO) {
        LocalDate currentDate = LocalDate.now().minusDays(1);
        return animalDTO.response().body().items().item().stream()
                .filter(animal -> isAnimalNoticeNotExpired(animal, currentDate))
                .count();
    }

    private boolean isAnimalNoticeNotExpired(PublicAnimalDTO.AnimalDTO animal, LocalDate currentDate) {
        return LocalDate.parse(animal.noticeEdt(), YEAR_HOUR_DAY_FORMAT).isAfter(currentDate);
    }

    private void updateShelterAddressByGoogle() {
        updateShelterAddress(googleService);
    }

    private void updateShelterAddress(GeocodingService geocodingService) {
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);
        for (Shelter shelter : shelters) {
            try {
                Coordinates coordinates = geocodingService.getCoordinates(shelter.getCareAddr());
                shelterRepository.updateAddressInfo(coordinates.lat(), coordinates.lng(), shelter.getId());
            } catch (Exception e) {
                log.warn("보호소의 위/경도 업데이트 실패 ,shelter {}: {}", shelter.getId(), e.getMessage());
            }
        }
    }

    private Flux<Shelter> fetchShelterDataFromApi(RegionCode regionCode, List<Long> savedShelterIds) {
        try {
            URI uri = buildShelterOpenApiURI(baseUrl, serviceKey, regionCode.getUprCd(), regionCode.getOrgCd());
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retry(3)
                    .flatMapMany(response -> convertResponseToNewShelter(response, regionCode, savedShelterIds))
                    .onErrorResume(e -> Flux.empty());
        } catch (Exception e) {
            log.warn("보소 데이터 패치를 위한 URI가 유효하지 않음, regionCode{}: {}", regionCode, e.getMessage());
            return Flux.empty();
        }
    }

    private Flux<Shelter> convertResponseToNewShelter(String response, RegionCode regionCode, List<Long> savedShelterIds) {
        return Mono.fromCallable(() -> parseJsonToItemDTO(response))
                .flatMapMany(Flux::fromIterable)
                .filter(itemDTO -> isNewShelter(itemDTO, savedShelterIds))
                .map(itemDTO -> itemDTO.toEntity(regionCode))
                .onErrorResume(e -> {
                    log.warn("보호소 데이터를 패치해오는 과정 중 파싱에서 에러 발생, regionCode {}: {}", regionCode, e.getMessage());
                    return Flux.empty();
                });
    }

    private List<PublicShelterDTO.itemDTO> parseJsonToItemDTO(String response) {
        return jsonParser.parse(response, PublicShelterDTO.class)
                .map(this::extractShelterItemDTOS)
                .orElse(Collections.emptyList());
    }

    private List<PublicShelterDTO.itemDTO> extractShelterItemDTOS(PublicShelterDTO shelterDTO) {
        return Optional.ofNullable(shelterDTO.response())
                .map(PublicShelterDTO.ResponseDTO::body)
                .map(PublicShelterDTO.BodyDTO::items)
                .map(PublicShelterDTO.ItemsDTO::item)
                .orElse(Collections.emptyList());
    }

    private boolean isNewShelter(PublicShelterDTO.itemDTO itemDTO, List<Long> existShelterIds) {
        return !existShelterIds.contains(itemDTO.careRegNo());
    }

    private List<Long> getUserLikedAnimalIds(Long userId) {
        return (userId != null) ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : Collections.emptyList();
    }
}