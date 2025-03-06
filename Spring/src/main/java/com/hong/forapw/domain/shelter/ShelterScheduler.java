package com.hong.forapw.domain.shelter;

import com.hong.forapw.common.utils.JsonParser;
import com.hong.forapw.domain.region.RegionCodeRepository;
import com.hong.forapw.domain.region.entity.RegionCode;
import com.hong.forapw.domain.shelter.model.PublicShelterDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.hong.forapw.common.utils.UriUtils.buildShelterOpenApiURI;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShelterScheduler {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final ShelterService shelterService;
    private final JsonParser jsonParser;
    private final WebClient webClient;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.shelter.uri}")
    private String baseUrl;

    public void updateNewShelters() {
        List<Long> savedShelterIds = shelterRepository.findAllIds();
        List<RegionCode> regionCodes = regionCodeRepository.findAll();

        Flux.fromIterable(regionCodes)
                .flatMap(regionCode -> fetchShelterDataFromApi(regionCode, savedShelterIds), 20)
                .onErrorContinue((ex, obj) -> log.warn("에러 발생! regionCode={}, err={}", obj, ex.getMessage()))
                .collectList()
                .flatMap(shelterList ->
                        Mono.fromCallable(() -> {
                            shelterService.saveSheltersTransaction(shelterList);
                            return "OK";
                        }).subscribeOn(Schedulers.boundedElastic())
                )
                .subscribe(
                        result -> log.info("Shelter 저장 완료: {}", result),
                        error -> log.error("Shelter 저장 중 오류 발생: {}", error.getMessage())
                );
    }

    private Flux<Shelter> fetchShelterDataFromApi(RegionCode regionCode, List<Long> savedShelterIds) {
        URI uri = buildShelterOpenApiURI(baseUrl, serviceKey, regionCode.getUprCd(), regionCode.getOrgCd());
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100))) // 최대 3번 100ms 간격으로 재시도
                .flatMapMany(response -> convertToNewShelter(response, regionCode, savedShelterIds))
                .onErrorResume(e -> {
                    log.warn("API 호출/파싱 에러 발생 - regionCode: {}, 에러: {}", regionCode, e.getMessage());
                    return Flux.empty();
                });
    }

    private Flux<Shelter> convertToNewShelter(String response, RegionCode regionCode, List<Long> savedShelterIds) {
        return Mono.fromCallable(() -> parseJsonToItemDTO(response))
                .flatMapMany(Flux::fromIterable)
                .filter(itemDTO -> isNewShelter(itemDTO, savedShelterIds))
                .map(itemDTO -> itemDTO.toEntity(regionCode))
                .onErrorResume(e -> {
                    log.warn("보호소 데이터를 패치해오는 과정 중 파싱에서 에러 발생, regionCode {}: {}", regionCode.getOrgName() + "" + regionCode.getUprName(), e.getMessage());
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
}