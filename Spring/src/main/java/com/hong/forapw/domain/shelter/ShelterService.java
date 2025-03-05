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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

import static com.hong.forapw.common.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.common.utils.PaginationUtils.isLastPage;
import static com.hong.forapw.common.utils.UriUtils.buildShelterOpenApiURI;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final AnimalRepository animalRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final JsonParser jsonParser;
    private final GoogleGeocodingService googleService;
    private final LikeService likeService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateShelter(List<AnimalJsonDTO> animalJsonResponse) {
        for (AnimalJsonDTO response : animalJsonResponse) {
            Shelter shelter = response.shelter();
            String animalJson = response.animalJson();
            updateShelterFromAnimalJson(animalJson, shelter);
        }

        updateShelterAddress(googleService);
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

    @Transactional
    public void saveSheltersTransaction(List<Shelter> shelterList) {
        shelterRepository.saveAll(shelterList);
    }

    private void updateShelterFromAnimalJson(String animalJson, Shelter shelter) {
        jsonParser.parse(animalJson, PublicAnimalDTO.class)
                .ifPresent(animalDTO ->
                        getFirstAnimalItem(animalDTO)
                                .ifPresent(firstAnimalItem -> shelterRepository.updateShelterInfo(
                                        firstAnimalItem.careTel(),
                                        firstAnimalItem.careAddr(),
                                        countActiveAnimals(animalDTO),
                                        shelter.getId()
                                ))
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

    private List<Long> getUserLikedAnimalIds(Long userId) {
        return (userId != null) ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : Collections.emptyList();
    }
}