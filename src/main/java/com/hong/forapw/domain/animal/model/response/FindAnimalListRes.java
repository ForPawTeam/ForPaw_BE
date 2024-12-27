package com.hong.forapw.domain.animal.model.response;

import java.util.List;

public record FindAnimalListRes(List<AnimalDTO> animals, boolean isLastPage) {
}
