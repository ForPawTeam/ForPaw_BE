package com.hong.forapw.domain.search.model.response;

import java.util.List;

public record SearchAllRes(
        List<ShelterDTO> shelters,
        List<PostDTO> posts,
        List<GroupDTO> groups) {
}
