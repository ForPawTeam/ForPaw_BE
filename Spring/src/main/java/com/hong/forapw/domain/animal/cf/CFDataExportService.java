package com.hong.forapw.domain.animal.cf;

import com.hong.forapw.domain.animal.model.InteractionDTO;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.integration.redis.RedisConstants.USER_ANIMAL_INTERACTION_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class CFDataExportService {

    private final RedisService redisService;

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

    public List<InteractionDTO> extractInteractionsForUser(Long userId) {
        Map<Object, Object> userInteractions = redisService.getHashEntries(USER_ANIMAL_INTERACTION_KEY, String.valueOf(userId));
        if (userInteractions == null) {
            return Collections.emptyList();
        }
        return userInteractions.entrySet().stream()
                .map(entry -> createInteractionDTO(userId, entry))
                .filter(Objects::nonNull)
                .toList();
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