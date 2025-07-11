package com.hong.forapw.common.utils;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class UriUtils {

    private UriUtils() {
    }

    public static String convertToHttpsUri(String url) {
        if (StringUtils.isBlank(url)) {
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);
        }

        String result = url;

        if (StringUtils.startsWithIgnoreCase(result, "http://")) {
            result = StringUtils.replaceOnce(result, "http://", "https://");
        }

        String oldPrefix = "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc";
        String newPrefix = "https://www.animal.go.kr";
        if (StringUtils.startsWithIgnoreCase(result, oldPrefix)) {
            result = newPrefix + result.substring(oldPrefix.length());
        }

        return result;
    }

    public static Mono<URI> buildAnimalOpenApiURI(String baseUri, String serviceKey, Long careRegNo) {
        String uri = baseUri + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";
        try {
            return Mono.just(new URI(uri));
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
            return Mono.empty();
        }
    }

    public static URI buildShelterOpenApiURI(String baseUri, String serviceKey, Integer uprCd, Integer orgCd) {
        String uri = baseUri + "?serviceKey=" + serviceKey + "&upr_cd=" + uprCd + "&org_cd=" + orgCd + "&_type=json";
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static URI buildKakaoGeocodingURI(String address, String kakaoGeoCodingURI) {
        if (address == null || address.isBlank())
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);

        return UriComponentsBuilder.fromHttpUrl(kakaoGeoCodingURI)
                .queryParam("query", address)
                .build()
                .encode()
                .toUri();
    }

    public static URI buildGoogleGeocodingURI(String address, String googleGeoCodingURI, String googleAPIKey) {
        if (address == null || address.isBlank())
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);

        return UriComponentsBuilder.fromHttpUrl(googleGeoCodingURI)
                .queryParam("address", address)
                .queryParam("key", googleAPIKey)
                .build()
                .encode()
                .toUri();
    }
}