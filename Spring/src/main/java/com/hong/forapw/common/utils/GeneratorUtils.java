package com.hong.forapw.common.utils;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneratorUtils {

    private GeneratorUtils() {
    }

    private static final String ALL_CHARS = "!@#$%^&*0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, 8) // 8자리
                .map(i -> random.nextInt(CHAR_SET.length()))
                .mapToObj(CHAR_SET::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    public static String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder()
                .append(pickRandomChar(random, 8, 0))  // 특수 문자
                .append(pickRandomChar(random, 10, 8)) // 숫자
                .append(pickRandomChar(random, 26, 18)) // 대문자
                .append(pickRandomChar(random, 26, 44)); // 소문자

        for (int i = 4; i < 8; i++) {
            password.append(pickRandomChar(random, ALL_CHARS.length(), 0));  // 나머지 문자 랜덤 추가
        }

        return shufflePassword(password);
    }

    private static char pickRandomChar(SecureRandom random, int range, int offset) {
        return ALL_CHARS.charAt(random.nextInt(range) + offset);
    }

    private static String shufflePassword(StringBuilder password) {
        List<Character> characters = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(characters);

        return characters.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}