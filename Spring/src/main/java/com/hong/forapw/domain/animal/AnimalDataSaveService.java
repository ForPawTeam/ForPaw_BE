package com.hong.forapw.domain.animal;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.JsonParser;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.animal.model.AnimalJsonDTO;
import com.hong.forapw.domain.animal.model.PublicAnimalDTO;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.domain.like.LikeService;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.shelter.Shelter;
import com.hong.forapw.domain.shelter.ShelterRepository;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hong.forapw.common.constants.GlobalConstants.ANIMAL_SEARCH_KEY;
import static com.hong.forapw.common.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalDataSaveService {

    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final ShelterRepository shelterRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final WebClient webClient;
    private final LikeService likeService;
    private final RedisService redisService;
    private final JsonParser jsonParser;

    @Value("${animal.names}")
    private String[] animalNames;

    @Value("${animal.update.uri}")
    private String updateAnimalIntroduceURI;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewAnimalData(List<AnimalJsonDTO> animalJsonResponse, List<Long> existingAnimalIds) {
        for (AnimalJsonDTO response : animalJsonResponse) {
            Shelter shelter = response.shelter();
            String animalJson = response.animalJson();

            List<Animal> animals = convertAnimalJsonToAnimals(animalJson, shelter, existingAnimalIds);
            animalRepository.saveAll(animals);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postProcessAfterAnimalUpdate() {
        List<Animal> expiredAnimals = handleExpiredAnimals();

        Set<Shelter> updatedShelters = getUpdatedShelters(expiredAnimals);
        updateShelterAnimalCounts(updatedShelters);

        updateAnimalIntroductions();
        resolveDuplicateShelters();
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

    private List<Animal> handleExpiredAnimals() {
        List<Animal> expiredAnimals = animalRepository.findAllOutOfDateWithShelter(LocalDateTime.now().toLocalDate());

        removeAnimalLikesFromCache(expiredAnimals);
        removeAnimalsFromUserSearchHistory(expiredAnimals);
        favoriteAnimalRepository.deleteByAnimalIn(expiredAnimals);
        animalRepository.deleteAll(expiredAnimals);

        return expiredAnimals;
    }

    private void removeAnimalLikesFromCache(List<Animal> expiredAnimals) {
        expiredAnimals.forEach(animal -> likeService.clearLikeCounts(animal.getId(), Like.ANIMAL));
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

    private Set<Shelter> getUpdatedShelters(List<Animal> expiredAnimals) {
        return expiredAnimals.stream()
                .map(Animal::getShelter)
                .collect(Collectors.toSet());
    }

    private void updateShelterAnimalCounts(Set<Shelter> updatedShelters) {
        updatedShelters.forEach(shelter ->
                shelter.updateAnimalCount(animalRepository.countByShelterId(shelter.getId()))
        );
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
}
