package com.hong.forapw.domain.region;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.forapw.domain.region.model.RegionsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionCodeService {

    private final RegionCodeRepository regionCodeRepository;
    private final ObjectMapper mapper;

    @Value("${region.code.file.path}")
    private String regionCodeFilePath;

    public void fetchRegionCode() {
        try {
            Optional.ofNullable(loadRegionDtoFromFile())
                    .map(RegionsDTO::toEntities)
                    .ifPresent(regionCodeRepository::saveAll);
        } catch (IOException e) {
            log.error("지역 코드 패치 실패: {}", regionCodeFilePath, e);
        }
    }

    private RegionsDTO loadRegionDtoFromFile() throws IOException {
        try (InputStream inputStream = TypeReference.class.getResourceAsStream(regionCodeFilePath)) {
            if (inputStream == null) {
                return null;
            }

            return mapper.readValue(inputStream, RegionsDTO.class);
        }
    }
}
