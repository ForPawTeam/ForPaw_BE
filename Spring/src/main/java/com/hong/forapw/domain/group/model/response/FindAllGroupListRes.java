package com.hong.forapw.domain.group.model.response;

import java.util.List;

public record FindAllGroupListRes(
        List<RecommendGroupDTO> recommendGroups,
        List<NewGroupDTO> newGroups,
        List<MyGroupDTO> myGroups) {
}