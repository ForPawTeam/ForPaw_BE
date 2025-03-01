package com.hong.forapw.domain.animal.cf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.forapw.domain.animal.model.InteractionDTO;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.common.constants.GlobalConstants.USER_ANIMAL_INTERACTION_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class CFDataExportService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${cf.export.uri}")
    private String exportUrl;

    private static final String FIELD_FORMAT = "like=%d,view=%d,inquiry=%d";

    public void updateInteraction(Long userId, Long animalId, int deltaLike, int deltaView, int deltaInquiry) {
        String field = String.valueOf(animalId);
        String oldValue = redisService.getHashValue(USER_ANIMAL_INTERACTION_KEY, String.valueOf(userId), field);

        int oldLike = 0, oldView = 0, oldInquiry = 0;
        if (oldValue != null) {
            Map<String, Integer> parsed = parseInteractionValue(oldValue);
            oldLike = parsed.getOrDefault("like", 0);
            oldView = parsed.getOrDefault("view", 0);
            oldInquiry = parsed.getOrDefault("inquiry", 0);
        }

        int newLike = Math.max(0, oldLike + deltaLike);
        int newView = Math.max(0, oldView + deltaView);
        int newInquiry = Math.max(0, oldInquiry + deltaInquiry);
        String newValue = String.format(FIELD_FORMAT, newLike, newView, newInquiry);

        redisService.setHashValue(USER_ANIMAL_INTERACTION_KEY, String.valueOf(userId), field, newValue);
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void exportInteractionsToFastAPI() {
        List<Long> userIds = userRepository.findAllIds();

        // 각 유저의 상호작용 데이터를 stream으로 변환하여 InteractionDTO 리스트 생성
        List<InteractionDTO> interactions = userIds.stream()
                .flatMap(userId -> extractInteractionsForUser(userId).stream())
                .collect(Collectors.toList());

        // FastAPI로 POST 요청 전송
        try {
            String jsonPayload = objectMapper.writeValueAsString(interactions);
            webClient.post()
                    .uri(exportUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(jsonPayload))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(e -> {
                        log.warn("FastAPI 호출 시 에러 발생: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (JsonProcessingException e) {
            log.error("상호작용 데이터를 JSON으로 변환 중 에러 발생", e);
        }
    }

    private List<InteractionDTO> extractInteractionsForUser(Long userId) {
        Map<Object, Object> userInteractions = redisService.getHashEntries(USER_ANIMAL_INTERACTION_KEY, String.valueOf(userId));
        if (userInteractions == null) {
            return Collections.emptyList();
        }
        return userInteractions.entrySet().stream()
                .map(entry -> createInteractionDTO(userId, entry))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private InteractionDTO createInteractionDTO(Long userId, Map.Entry<Object, Object> entry) {
        try {
            Long animalId = Long.valueOf(entry.getKey().toString());
            Map<String, Integer> parsedData = parseInteractionValue(entry.getValue().toString());
            return new InteractionDTO(parsedData, userId, animalId);
        } catch (Exception e) {
            log.error("Interaction 데이터 생성 중 에러 발생: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Integer> parseInteractionValue(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(part -> part.split("="))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(
                        kv -> kv[0].trim(),
                        kv -> Integer.parseInt(kv[1].trim())
                ));
    }
}