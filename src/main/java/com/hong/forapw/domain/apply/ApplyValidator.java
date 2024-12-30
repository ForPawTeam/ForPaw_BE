package com.hong.forapw.domain.apply;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.entity.Animal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplyValidator {

    private final ApplyRepository applyRepository;


    public void validateNoPreviousApplication(Long userId, Long animalId) {
        if (doesAlreadyApplied(userId, animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_APPLIED);
        }
    }

    public void validateUserIsApplicant(Long applyId, Long userId) {
        if (isNotApplicant(applyId, userId)) {
            throw new CustomException(ExceptionCode.APPLICATION_NOT_FOUND);
        }
    }

    private boolean doesAlreadyApplied(Long userId, Long animalId) {
        return applyRepository.existsByAnimalIdAndUserId(animalId, userId);
    }

    private boolean isNotApplicant(Long applyId, Long userId) {
        return !applyRepository.existsByApplyIdAndUserId(applyId, userId);
    }
}