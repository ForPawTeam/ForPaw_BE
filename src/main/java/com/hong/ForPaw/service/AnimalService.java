package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.AnimalResponse;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.controller.DTO.AniamlJsonDTO;
import com.hong.ForPaw.domain.Animal;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.AnimalRepository;
import com.hong.ForPaw.repository.FavoriteRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;
    private final FavoriteRepository favoriteRepository;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.animalURL}")
    private String baseUrl;

    @Value("${animal.names}")
    private String[] animalNames;

    @Transactional
    public void loadAnimalDate() {
        // shelter 돌면서 => animal 정보 쭉 떙겨온다 => shelter를 패치하고 => animal 등록
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        List<Shelter> shelters = shelterRepository.findAll();

        for(Shelter shelter : shelters){
            Long careRegNo = shelter.getCareRegNo();

            try {
                String url = baseUrl + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "*/*;q=0.9"); // HTTP_ERROR 방지
                HttpEntity<?> entity = new HttpEntity<>(null, headers);

                URI uri = new URI(url);

                ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
                String response = responseEntity.getBody();
                System.out.println(response);

                AniamlJsonDTO json = mapper.readValue(response, AniamlJsonDTO.class);
                List<AniamlJsonDTO.ItemDTO> itemDTOS = json.response().body().items().item();
                boolean isShelterUpdate = false; // 보호소 업데이트 여부 (동물을 조회할 때 보호소 나머지 정보도 등장함)

                for (AniamlJsonDTO.ItemDTO itemDTO : itemDTOS) {
                    if (!isShelterUpdate) {
                        shelter.updateShelterInfo(itemDTO.careTel(), itemDTO.careAddr());
                        isShelterUpdate = true;
                    }

                    Animal animal = Animal.builder()
                            .shelter(shelter) // 연관관계 매핑
                            .desertionNo(Long.valueOf(itemDTO.desertionNo()))
                            .happenDt(LocalDate.parse(itemDTO.happenDt(), formatter))
                            .happenPlace(itemDTO.happenPlace())
                            .kind(itemDTO.kindCd())
                            .color(itemDTO.colorCd())
                            .age(itemDTO.age())
                            .weight(itemDTO.weight())
                            .noticeSdt(LocalDate.parse(itemDTO.noticeSdt(), formatter))
                            .noticeEdt(LocalDate.parse(itemDTO.noticeEdt(), formatter))
                            .profileURL(itemDTO.popfile())
                            .processState(itemDTO.processState())
                            .gender(itemDTO.sexCd())
                            .neuter(itemDTO.neuterYn())
                            .specialMark(itemDTO.specialMark())
                            .build();

                    animalRepository.save(animal);
                }
            }
            catch (Exception e){
                System.err.println("JSON 파싱 오류가 발생했습니다. 재시도 중...: ");
                System.out.println(e);
            }
        }
    }

    @Transactional
    public AnimalResponse.FindAllAnimalsDTO findAllAnimals(Pageable pageable, User user){

        Page<Animal> animalPage = animalRepository.findAll(pageable);

        List<AnimalResponse.FindAllAnimalsDTO.AnimalDTO> animalDTOS = animalPage.getContent().stream()
                .map(animal -> new AnimalResponse.FindAllAnimalsDTO.AnimalDTO(animal.getDesertionNo(), getAnimalName(), animal.getAge()
                        , animal.getGender(), animal.getSpecialMark(), animal.getShelter().getRegionCode().getUprName()+" "+animal.getShelter().getRegionCode().getOrgName()
                        , animal.getInquiryNum(), animal.getLikeNum(), favoriteRepository.findByUserAndAnimal(user, animal).isPresent(), animal.getProfileURL() ))
                .collect(Collectors.toList());


        return new AnimalResponse.FindAllAnimalsDTO(animalDTOS);
    }

    // 동물 이름 지어주는 메서드
    public String getAnimalName() {
        int index = ThreadLocalRandom.current().nextInt(animalNames.length);
        return animalNames[index];
    }
}