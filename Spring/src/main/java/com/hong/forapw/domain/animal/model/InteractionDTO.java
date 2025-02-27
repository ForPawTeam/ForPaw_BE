package com.hong.forapw.domain.animal.model;

import java.util.Map;

public record InteractionDTO(
        Long user_id,
        Long animal_id,
        int like_count,
        int view_count,
        int inquiry_count) {

    public InteractionDTO( Map<String, Integer> parsed, Long userId, Long animalId) {
        this(
                userId,
                animalId,
                parsed.getOrDefault("like", 0),
                parsed.getOrDefault("view", 0),
                parsed.getOrDefault("inquiry", 0)
        );
    }
}