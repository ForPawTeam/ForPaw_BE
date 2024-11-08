package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.*;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Animal.AnimalType;
import com.hong.ForPaw.domain.Apply.Apply;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.Animal.FavoriteAnimal;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.*;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Animal.FavoriteAnimalRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final RedisService redisService;
    private final ApplyRepository applyRepository;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;
    private final WebClient webClient;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.animal.uri}")
    private String animalURI;

    @Value("${animal.names}")
    private String[] animalNames;

    @Value("${kakao.key}")
    private String kakaoAPIKey;

    @Value("${kakao.map.geocoding.uri}")
    private String kakaoGeoCodingURI;

    @Value("${google.map.geocoding.uri}")
    private String googleGeoCodingURI;

    @Value("${google.api.key}")
    private String googleAPIKey;

    @Value("${recommend.uri}")
    private String animalRecommendURI;

    @Value("${animal.update.uri}")
    private String updateAnimalIntroduceURI;

    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final String ANIMAL_SEARCH_KEY_PREFIX = "animalSearch";
    private static final String ANIMAL_TYPE_DOG = "[개]";
    private static final String ANIMAL_TYPE_CAT = "[고양이]";
    public static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달
    private static final Map<String, AnimalType> ANIMAL_TYPE_MAP = Map.of("dog", AnimalType.DOG, "cat", AnimalType.CAT, "other", AnimalType.OTHER);

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void updateAnimalData() {
        List<Long> existAnimalIds = animalRepository.findAllIds();
        List<Shelter> shelters = shelterRepository.findAllWithRegionCode();

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(75)) // 각 요청 사이에 0.075초 지연
                .flatMap(shelter -> {
                    Long careRegNo = shelter.getId();
                    URI uri = buildAnimalOpenApiURI(serviceKey, careRegNo);

                    return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)
                            .retry(3)
                            .flatMap(response -> updateShelterByAnimalData(response, shelter) // 동물 데이터로 보호소 업데이트
                                    .then(Mono.just(response)))
                            .flatMapMany(response -> convertResponseToAnimal(response, shelter, existAnimalIds) // 동물 엔티티 컬렉션으로 변환
                                    .onErrorResume(e -> Flux.empty()))
                            .collectList()
                            .doOnNext(animalRepository::saveAll);
                })
                .then()
                .doOnTerminate(this::updateShelterAddressByGoogle) // loadAnimalData가 완료되면 updateShelterAddressByGoogle 실행
                .subscribe();
    }

    @Transactional
    public void updateShelterAddressByGoogle(){
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(50))
                .flatMap(shelter -> {
                    URI uri = buildGoogleGeocodingURI(shelter.getCareAddr());
                    return webClient.get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(GoogleMapDTO.MapDTO.class)
                            .flatMap(mapDTO -> Mono.justOrEmpty(mapDTO.results().stream().findFirst()))
                            .doOnNext(resultDTO -> {
                                shelterRepository.updateAddressInfo(resultDTO.geometry().location().lat(), resultDTO.geometry().location().lng(), shelter.getId());
                            });
                })
                .then()
                .doOnTerminate(this::postProcessAfterAnimalUpdate)
                .subscribe();
    }

    // 동물 업데이트 후 후처리
    @Transactional
    public void postProcessAfterAnimalUpdate(){
        Set<Shelter> updatedShelters = new HashSet<>();
        List<Animal> expiredAnimals = animalRepository.findAllOutOfDateWithShelter(LocalDateTime.now().toLocalDate());

        // 1. 캐싱한 '좋아요 수' 삭제
        expiredAnimals.forEach(animal -> {
            updatedShelters.add(animal.getShelter());
            redisService.removeData(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId().toString());
        });

        // 2. 유저가 검색한 동물 기록에서 삭제
        List<User> users = userRepository.findAll();
        for(User user : users){
            expiredAnimals.forEach(animal -> {
                String key = ANIMAL_SEARCH_KEY_PREFIX + ":" + user.getId();
                redisService.removeListElement(key, animal.getId().toString());
            });
        }

        // 3. 공지가 만료된 유기 동물 삭제
        favoriteAnimalRepository.deleteByAnimalIn(expiredAnimals);
        animalRepository.deleteAll(expiredAnimals);

        // 4. 보호소의 '보호 동물 수' 업데이트
        updatedShelters.forEach(shelter ->
                shelter.updateAnimalCnt(animalRepository.countByShelterId(shelter.getId()))
        );

        // 5. 소개글 업데이트
        requestAnimalIntroduction()
                .doOnError(error -> log.info("소개글 업데이트 요청 실패"))
                .subscribe();

        // 6. 중복 보호소 처리
        processDuplicateShelters();
    }

    @Transactional
    public void updateShelterAddressByKakao(){
        List<Shelter> shelters = shelterRepository.findByAnimalCntGreaterThan(0L);

        Flux.fromIterable(shelters)
                .delayElements(Duration.ofMillis(50))
                .flatMap(shelter -> {
                    URI uri = buildKakaoGeocodingURI(shelter.getCareAddr());
                    return webClient.get()
                            .uri(uri)
                            .header("Authorization", "KakaoAK " + kakaoAPIKey)
                            .retrieve()
                            .bodyToMono(KakaoMapDTO.MapDTO.class)
                            .flatMap(mapDTO -> Mono.justOrEmpty(mapDTO.documents().stream().findFirst()))
                            .doOnNext(document -> {
                                shelterRepository.updateAddressInfo(Double.valueOf(document.y()), Double.valueOf(document.x()), shelter.getId());
                            });
                })
                .subscribe();
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindAnimalListDTO findAnimalList(String type, Long userId, Pageable pageable){
        // sort 파라미터를 AnimalType으로 변환
        AnimalType animalType = converStringToAnimalType(type);
        Page<Animal> animalPage = animalRepository.findByAnimalType(animalType, pageable);
        boolean isLastPage = !animalPage.hasNext();

        // 사용자가 '좋아요' 표시한 Animal의 ID 목록 => 만약 로그인 되어 있지 않다면, 빈 리스트로 처리한다.
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> {
                    Long likeNum = getCachedLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
                    return new AnimalResponse.AnimalDTO(
                        animal.getId(),
                        animal.getName(),
                        animal.getAge(),
                        animal.getGender(),
                        animal.getSpecialMark(),
                        animal.getKind(),
                        animal.getWeight(),
                        animal.getNeuter(),
                        animal.getProcessState(),
                        animal.getRegion(),
                        animal.getInquiryNum(),
                        likeNum,
                        likedAnimalIds.contains(animal.getId()),
                        animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindAnimalListDTO(animalDTOS, isLastPage);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindRecommendedAnimalList findRecommendedAnimalList(Long userId){
        // 추천 동물 ID 목록
        List<Long> recommendedAnimalIds = getRecommendedAnimalIdList(userId);
        List<Long> likedAnimalIds = userId != null ? favoriteAnimalRepository.findAnimalIdsByUserId(userId) : new ArrayList<>();

        List<AnimalResponse.AnimalDTO> animalDTOS = animalRepository.findByIds(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeNum = getCachedLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
                    return new AnimalResponse.AnimalDTO(
                            animal.getId(),
                            animal.getName(),
                            animal.getAge(),
                            animal.getGender(),
                            animal.getSpecialMark(),
                            animal.getKind(),
                            animal.getWeight(),
                            animal.getNeuter(),
                            animal.getProcessState(),
                            animal.getRegion(),
                            animal.getInquiryNum(),
                            likeNum,
                            likedAnimalIds.contains(animal.getId()),
                            animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindRecommendedAnimalList(animalDTOS);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindLikeAnimalListDTO findLikeAnimalList(Long userId){
        List<Animal> animalPage = favoriteAnimalRepository.findAnimalsByUserId(userId);

        List<AnimalResponse.AnimalDTO> animalDTOS = animalPage.stream()
                .map(animal -> {
                    Long likeNum = getCachedLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
                    return new AnimalResponse.AnimalDTO(
                            animal.getId(),
                            animal.getName(),
                            animal.getAge(),
                            animal.getGender(),
                            animal.getSpecialMark(),
                            animal.getKind(),
                            animal.getWeight(),
                            animal.getNeuter(),
                            animal.getProcessState(),
                            animal.getRegion(),
                            animal.getInquiryNum(),
                            likeNum,
                            true,
                            animal.getProfileURL());
                })
                .collect(Collectors.toList());

        return new AnimalResponse.FindLikeAnimalListDTO(animalDTOS);
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindAnimalByIdDTO findAnimalById(Long animalId, Long userId){
        // 로그인을 한 경우, 추천을 위해 검색한 동물의 id 저장 (5개까지만 저장된다)
        if(userId != null){
            String key = ANIMAL_SEARCH_KEY_PREFIX + ":" + userId;
            redisService.addListElementWithLimit(key, animalId.toString(), 5L);
        }

        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        boolean isLike = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animal.getId()).isPresent();

        return new AnimalResponse.FindAnimalByIdDTO(animalId,
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getRegion(),
                isLike,
                animal.getProfileURL(),
                animal.getHappenPlace(),
                animal.getKind(),
                animal.getColor(),
                animal.getWeight(),
                animal.getNoticeSdt(),
                animal.getNoticeEdt(),
                animal.getProcessState(),
                animal.getNeuter(),
                animal.getIntroductionTitle(),
                animal.getIntroductionContent(),
                animal.isAdopted());
    }

    @Transactional
    public void likeAnimal(Long userId, Long animalId){
        // 존재하지 않는 동물이면 에러
        if(!animalRepository.existsById(animalId)){
                throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
            }

        Optional<FavoriteAnimal> favoriteAnimalOP = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animalId);

        // 좋아요가 이미 있다면 삭제, 없다면 추가
        if (favoriteAnimalOP.isPresent()) {
            favoriteAnimalRepository.delete(favoriteAnimalOP.get());
            redisService.decrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
        }
        else {
            Animal animalRef = entityManager.getReference(Animal.class, animalId);
            User userRef = entityManager.getReference(User.class, userId);

            FavoriteAnimal favoriteAnimal = FavoriteAnimal.builder()
                    .user(userRef)
                    .animal(animalRef)
                    .build();

            favoriteAnimalRepository.save(favoriteAnimal);
            redisService.incrementValue(ANIMAL_LIKE_NUM_KEY_PREFIX, animalId.toString(), 1L);
        }
    }

    @Transactional
    public AnimalResponse.CreateApplyDTO applyAdoption(AnimalRequest.ApplyAdoptionDTO requestDTO, Long userId, Long animalId){
        // 동물이 존재하지 않으면 에러
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        // 이미 입양된 동물이면 에러
        if(animal.isAdopted()){
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
        }

        // 이미 지원하였으면 에러
        if(applyRepository.existsByUserIdAndAnimalId(userId, animalId)){
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_APPLY);
        }

        User userRef = entityManager.getReference(User.class, userId);
        Apply apply = Apply.builder()
                .user(userRef)
                .animal(animal)
                .status(ApplyStatus.PROCESSING)
                .name(requestDTO.name())
                .tel(requestDTO.tel())
                .roadNameAddress(requestDTO.roadNameAddress())
                .addressDetail(requestDTO.addressDetail())
                .zipCode(requestDTO.zipCode())
                .build();

        applyRepository.save(apply);

        // 동물의 문의 횟수 증가
        animalRepository.incrementInquiryNumById(animalId);

        return new AnimalResponse.CreateApplyDTO(apply.getId());
    }

    @Transactional(readOnly = true)
    public AnimalResponse.FindApplyListDTO findApplyList(Long userId){
        List<Apply> applies = applyRepository.findAllByUserIdWithAnimal(userId);

        List<AnimalResponse.ApplyDTO> applyDTOS = applies.stream()
                .map(apply -> new AnimalResponse.ApplyDTO(
                        apply.getId(),
                        apply.getAnimal().getId(),
                        apply.getAnimal().getName(),
                        apply.getAnimal().getKind(),
                        apply.getAnimal().getGender(),
                        apply.getAnimal().getAge(),
                        apply.getName(),
                        apply.getTel(),
                        apply.getRoadNameAddress(),
                        apply.getAddressDetail(),
                        apply.getZipCode(),
                        apply.getStatus()))
                .collect(Collectors.toList());

        return new AnimalResponse.FindApplyListDTO(applyDTOS);
    }

    @Transactional
    public void updateApply(AnimalRequest.UpdateApplyDTO requestDTO, Long applyId, Long userId){
        // 지원하지 않았거나, 권한이 없으면 에러
        if(!applyRepository.existsByApplyIdAndUserId(applyId, userId)){
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        Apply apply = applyRepository.findById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.APPLY_NOT_FOUND)
        );

        apply.updateApply(requestDTO.name(),
                requestDTO.tel(),
                requestDTO.roadNameAddress(),
                requestDTO.addressDetail(),
                requestDTO.zipCode());
    }

    @Transactional
    public void deleteApply(Long applyId, Long userId){
        // 지원하지 않았거나, 권한이 없으면 에러
        if(!applyRepository.existsByApplyIdAndUserId(applyId, userId)){
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        // 동물의 문의 횟수 감소
        Animal animal = applyRepository.findAnimalIdById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );
        animal.decrementInquiryNum();

        applyRepository.deleteById(applyId);
    }

    private Flux<Animal> convertResponseToAnimal(String response, Shelter shelter, List<Long> existAnimalIds) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        return Mono.fromCallable(() -> {
            AnimalDTO json = mapper.readValue(response, AnimalDTO.class);

            return Optional.ofNullable(json.response().body().items())
                    .map(AnimalDTO.ItemsDTO::item)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(itemDTO -> !existAnimalIds.contains(Long.valueOf(itemDTO.desertionNo()))) // 이미 저장되어 있는 동물은 업데이트 하지 않는다
                    .filter(itemDTO -> LocalDate.parse(itemDTO.noticeEdt(), formatter).isAfter(LocalDate.now())) // 공고 종료가 된 것은 필터링
                    .map(itemDTO -> buildAnimal(itemDTO, shelter))
                    .collect(Collectors.toList());
        }).flatMapMany(Flux::fromIterable);
    }

    private Mono<Void> updateShelterByAnimalData(String response, Shelter shelter) {
        return Mono.fromCallable(() -> {
            AnimalDTO json = mapper.readValue(response, AnimalDTO.class);
            Optional.ofNullable(json)
                    .map(j -> j.response().body().items().item())
                    .filter(items -> !items.isEmpty())
                    .map(items -> items.get(0))
                    .ifPresent(itemDTO -> shelterRepository.updateShelterInfo(itemDTO.careTel(), itemDTO.careAddr(), countActiveAnimals(json), shelter.getId()));
            return Mono.empty();
        }).then();
    }

    private Animal buildAnimal(AnimalDTO.ItemDTO itemDTO, Shelter shelter) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        return Animal.builder()
                .id(Long.valueOf(itemDTO.desertionNo()))
                .name(createAnimalName())
                .shelter(shelter)
                .happenDt(LocalDate.parse(itemDTO.happenDt(), formatter))
                .happenPlace(itemDTO.happenPlace())
                .kind(parseSpecies(itemDTO.kindCd()))
                .category(parseAnimalType(itemDTO.kindCd()))
                .color(itemDTO.colorCd())
                .age(itemDTO.age())
                .weight(itemDTO.weight())
                .noticeSdt(LocalDate.parse(itemDTO.noticeSdt(), formatter))
                .noticeEdt(LocalDate.parse(itemDTO.noticeEdt(), formatter))
                .profileURL(convertHttpToHttps(itemDTO.popfile()))
                .processState(itemDTO.processState())
                .gender(itemDTO.sexCd())
                .neuter(itemDTO.neuterYn())
                .specialMark(itemDTO.specialMark())
                .region(shelter.getRegionCode().getUprName() + " " + shelter.getRegionCode().getOrgName())
                .introductionContent("소개글을 작성중입니다!")
                .build();
    }

    public AnimalType parseAnimalType(String input) {
        if (input.startsWith(ANIMAL_TYPE_DOG)) {
            return AnimalType.DOG;
        } else if (input.startsWith(ANIMAL_TYPE_CAT)) {
            return AnimalType.CAT;
        } else {
            return AnimalType.OTHER;
        }
    }

    // 동물 이름 지어주는 메서드
    public String createAnimalName() {
        int index = ThreadLocalRandom.current().nextInt(animalNames.length);
        return animalNames[index];
    }

    private URI buildAnimalOpenApiURI(String serviceKey, Long careRegNo) {
        String url = animalURI + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";

        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private URI buildKakaoGeocodingURI(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(kakaoGeoCodingURI);
        uriBuilder.queryParam("query", address);

        return uriBuilder.build().encode().toUri();
    }

    private URI buildGoogleGeocodingURI(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(googleGeoCodingURI);
        uriBuilder.queryParam("address", address);
        uriBuilder.queryParam("key", googleAPIKey);

        return uriBuilder.build().encode().toUri();
    }

    public List<Long> getRecommendedAnimalIdList(Long userId){
        Pageable pageable = PageRequest.of(0, 5);

        // 로그인 되지 않았으면, 추천을 할 수 없으니 그냥 최신순 반환
        if (userId == null) {
            return animalRepository.findAllIds(pageable).getContent();
        }

        Map<String, Long> requestBody = Map.of("user_id", userId);
        List<Long> recommendedAnimalIds = webClient.post()
                .uri(animalRecommendURI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(AnimalResponse.RecommendationDTO.class)
                .map(AnimalResponse.RecommendationDTO::recommendedAnimals)
                .onErrorResume(e -> {
                    log.info("FastAPI 호술 시 에러 발생: " + e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();

        // 조회 기록이 없어서, 추천하는 ID 목록이 없으면 사용자 위치 기반으로 가져온다
        if (recommendedAnimalIds.isEmpty()) {
            recommendedAnimalIds = findAnimalIdListByUserLocation(userId);
        }

        return recommendedAnimalIds;
    }

    private Mono<Void> requestAnimalIntroduction() {
        return webClient.post()
                .uri(updateAnimalIntroduceURI)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(httpStatus ->(httpStatus.is5xxServerError() || httpStatus.is4xxClientError()),
                        clientResponse -> Mono.error(new RuntimeException("소개글 요청 실패")))
                .bodyToMono(Void.class)
                .retryWhen(
                        Retry.fixedDelay(3, Duration.ofSeconds(2))  // 2초 간격으로 3번 재시도
                                .filter(throwable -> throwable instanceof RuntimeException)  // RuntimeException 발생 시 재시도
                );
    }

    private List<Long> findAnimalIdListByUserLocation(Long userId) {
        PageRequest pageRequest = PageRequest.of(0, 5);

        // 우선 사용자 district를 바탕으로 조회
        List<Long> animalIds = userRepository.findDistrictById(userId)
                .map(district -> animalRepository.findIdsByDistrict(district, pageRequest))
                .orElseGet(ArrayList::new);

        // 조회된 동물 ID의 수가 5개 미만인 경우, province 범위까지 확대해서 추가 조회
        if (animalIds.size() < 5) {
            animalIds.addAll(userRepository.findProvinceById(userId)
                    .map(province -> animalRepository.findIdsByProvince(province, pageRequest))
                    .orElseGet(ArrayList::new));

            return animalIds.subList(0, Math.min(5, animalIds.size()));
        }

        return animalIds;
    }

    private AnimalType converStringToAnimalType(String type) {
        if(type.equals("date")) {
            return null;
        }

        return Optional.ofNullable(ANIMAL_TYPE_MAP.get(type))
                .orElseThrow(() -> new CustomException(ExceptionCode.WRONG_ANIMAL_TYPE));
    }

    private Long countActiveAnimals(AnimalDTO json) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        return json.response().body().items().item().stream()
                .filter(itemDTO -> LocalDate.parse(itemDTO.noticeEdt(), formatter).isAfter(yesterday))
                .count();
    }

    // 구체적인 종 파싱
    private static String parseSpecies(String input) {
        Pattern pattern = Pattern.compile("\\[.*?\\] (.+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    private void processDuplicateShelters() {
        List<Shelter> duplicateShelters = shelterRepository.findDuplicateShelters();

        Map<String, List<Shelter>> dupliShelterMap = duplicateShelters.stream()
                .collect(Collectors.groupingBy(Shelter::getCareTel));

        dupliShelterMap.values().stream()
                .filter(shelters -> shelters.size() > 1) // 중복된 보호소만 처리
                .forEach(shelters -> {
                    // isDuplicate가 false인 첫 번째 Shelter를 targetShelter로 지정
                    Shelter targetShelter = shelters.stream()
                            .filter(shelter -> !shelter.isDuplicate())
                            .findFirst()
                            .orElse(shelters.get(0));

                    // 중복된 보호소의 모든 동물들을 targetShelter로 이동
                    shelters.stream()
                            .filter(duplicateShelter -> !duplicateShelter.equals(targetShelter))
                            .forEach(duplicateShelter -> {
                                List<Animal> animalsToMove = animalRepository.findByShelter(duplicateShelter);
                                animalsToMove.forEach(animal -> animalRepository.updateShelter(targetShelter, animal.getId()));

                                // 중복된 shelter를 중복 상태로 업데이트
                                shelterRepository.updateIsDuplicate(duplicateShelter.getId(), true);
                            });

                    // targetShelter가 중복된 상태가 아닌 경우 이를 갱신
                    shelterRepository.updateIsDuplicate(targetShelter.getId(), false);
                });
    }

    private Long getCachedLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), ANIMAL_EXP);
        }

        return likeNum;
    }

    public String convertHttpToHttps(String url) {
        if (Objects.requireNonNull(url).startsWith("http://")) {
            return url.replaceFirst("http://", "https://");
        }

        return url;
    }
}