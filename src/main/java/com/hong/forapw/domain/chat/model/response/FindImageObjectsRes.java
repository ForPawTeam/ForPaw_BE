package com.hong.forapw.domain.chat.model.response;

import java.util.List;

public record FindImageObjectsRes(
        List<ImageObjectDTO> images,
        boolean isLastPage) {
}