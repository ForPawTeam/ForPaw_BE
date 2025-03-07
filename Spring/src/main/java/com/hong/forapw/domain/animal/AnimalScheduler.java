package com.hong.forapw.domain.animal;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.model.AnimalJsonDTO;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.shelter.Shelter;
import com.hong.forapw.domain.shelter.ShelterRepository;
import com.hong.forapw.domain.shelter.ShelterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

import static com.hong.forapw.common.utils.UriUtils.buildAnimalOpenApiURI;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalScheduler {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final ShelterService shelterService;
    private final AnimalService animalService;
    private final WebClient webClient;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.animal.uri}")
    private String animalURI;

    @Value("${animal.update.uri}")
    private String updateAnimalIntroduceURI;

    @Scheduled(cron = "0 0 0 * * *")
    public void updateNewAnimals() {
        List<Long> existingAnimalIds = animalRepository.findAllIds();
        List<Shelter> shelters = shelterRepository.findAllWithRegionCode();

        fetchAnimalDataFromApi(shelters)
                .collectList()
                .flatMap(animalJsonDTOs ->  // Mono.fromRunnable을 통해 동기 메서드들을 한 번에 실행
                        Mono.fromRunnable(() -> processAnimalUpdate(animalJsonDTOs, existingAnimalIds))
                                .subscribeOn(Schedulers.boundedElastic())) // 블로킹 I/O → boundedElastic 스레드풀에서 실행
                .subscribe(
                        result -> log.info("동물 정보 업데이트 완료: {}", result),
                        error -> log.error("updateNewAnimals 중 오류 발생: {}", error.getMessage())
                );
    }

    private Flux<AnimalJsonDTO> fetchAnimalDataFromApi(List<Shelter> shelters) {
        return Flux.fromIterable(shelters)
                .flatMap(shelter -> buildAnimalOpenApiURI(animalURI, serviceKey, shelter.getId())
                                .flatMap(uri -> webClient.get()
                                        .uri(uri)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                                        .map(rawJson -> new AnimalJsonDTO(shelter, rawJson)))
                                .onErrorResume(e -> {
                                    log.error("Shelter {} 데이터 가져오기 실패: {}", shelter.getId(), e.getMessage());
                                    return Mono.empty();
                                }),
                        20 // concurrency 파라미터
                );
    }

    private void processAnimalUpdate(List<AnimalJsonDTO> animalJsonDTOs, List<Long> existingAnimalIds) {
        animalService.saveNewAnimalData(animalJsonDTOs, existingAnimalIds);
        shelterService.updateShelter(animalJsonDTOs);
        animalService.postProcessAfterAnimalUpdate();
        updateAnimalIntroductions();
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
}
