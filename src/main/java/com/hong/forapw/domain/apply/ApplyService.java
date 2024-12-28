package com.hong.forapw.domain.apply;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.apply.model.request.ApplyAdoptionReq;
import com.hong.forapw.domain.apply.model.request.UpdateApplyReq;
import com.hong.forapw.domain.apply.model.response.CreateApplyRes;
import com.hong.forapw.domain.apply.model.response.FindApplyListRes;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApplyService {

    private final ApplyRepository applyRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;


    @Transactional
    public CreateApplyRes applyAdoption(ApplyAdoptionReq request, Long userId, Long animalId) {
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        validateAnimalNotAdopted(animal);
        validateNoPreviousApplication(userId, animalId);

        User user = userRepository.getReferenceById(userId);
        Apply apply = request.fromEntity(user, animal);
        applyRepository.save(apply);

        animal.incrementInquiryNum();

        return new CreateApplyRes(apply.getId());
    }

    public FindApplyListRes findApplyList(Long userId) {
        List<Apply> applies = applyRepository.findAllByUserIdWithAnimal(userId);

        List<FindApplyListRes.ApplyDTO> applyDTOS = applies.stream()
                .map(FindApplyListRes.ApplyDTO::new)
                .toList();

        return new FindApplyListRes(applyDTOS);
    }

    @Transactional
    public void updateApply(UpdateApplyReq request, Long applyId, Long userId) {
        validateUserIsApplicant(applyId, userId);

        Apply apply = applyRepository.findById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.APPLICATION_NOT_FOUND)
        );

        apply.updateApply(
                request.name(),
                request.tel(),
                request.roadNameAddress(),
                request.addressDetail(),
                request.zipCode()
        );
    }

    @Transactional
    public void deleteApply(Long applyId, Long userId) {
        validateUserIsApplicant(applyId, userId);

        Animal animal = applyRepository.findAnimalIdById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );
        animal.decrementInquiryNum();

        applyRepository.deleteById(applyId);
    }

    private void validateAnimalNotAdopted(Animal animal) {
        if (animal.isAdopted()) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_ADOPTED);
        }
    }

    private void validateNoPreviousApplication(Long userId, Long animalId) {
        if (applyRepository.existsByUserIdAndAnimalId(userId, animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_APPLIED);
        }
    }

    private void validateUserIsApplicant(Long applyId, Long userId) {
        if (!applyRepository.existsByApplyIdAndUserId(applyId, userId)) {
            throw new CustomException(ExceptionCode.APPLICATION_NOT_FOUND);
        }
    }
}
