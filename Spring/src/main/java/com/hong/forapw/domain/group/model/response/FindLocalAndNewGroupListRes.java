package com.hong.forapw.domain.group.model.response;

import java.util.List;

public record FindLocalAndNewGroupListRes(
        List<LocalGroupDTO> localGroups,
        List<NewGroupDTO> newGroups) {
}