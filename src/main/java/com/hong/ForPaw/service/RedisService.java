package com.hong.ForPaw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // 데이터 저장 (유효 기간 존재)
    public void storeValue(String type, String id, String value, Long expirationTime) {
        redisTemplate.opsForValue().set(buildKey(type, id), value, expirationTime, TimeUnit.MILLISECONDS);
    }

    // 유효 기간 X
    public void storeValue(String type, String id, String value) {
        redisTemplate.opsForValue().set(buildKey(type, id), value);
    }

    public void addSetElement(String key, Long value) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        setOps.add(key, String.valueOf(value));
    }

    public void addSetElement(String key, Long value, Long expirationTime) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        setOps.add(key, String.valueOf(value));
        redisTemplate.expire(key, expirationTime, TimeUnit.SECONDS);
    }

    public Set<String> getAllElement(String key) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        return setOps.members(key);
    }

    public void addListElementWithLimit(String key, String value, Long limit) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.leftPush(key, value);

        // 리스트 크기가 5를 초과하는 경우, 가장 오래된 항목(리스트의 끝) 제거
        while (listOps.size(key) > limit) {
            listOps.rightPop(key);
        }
    }

    public void deleteListElement(String key, String value){
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.remove(key, 0, value);
    }

    public void incrementCnt(String type, String id, Long cnt){
        redisTemplate.opsForValue().increment(buildKey(type, id), cnt);
    }

    public void decrementCnt(String type, String id, Long cnt){
        redisTemplate.opsForValue().decrement(buildKey(type, id), cnt);
    }

    public void setExpireDate(String type, String id, Long expirationTime){
        redisTemplate.expire(buildKey(type, id), expirationTime, TimeUnit.MILLISECONDS);
    }

    // 데이터 존재 여부
    public boolean isDateExist(String type, String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(type, id)));
    }

    // 저장된 데이터와 일치여부 비교
    public boolean validateData(String type, String id, String value){
        String storedData = redisTemplate.opsForValue().get(buildKey(type, id));

        if(storedData == null){
            return false;
        }
        return storedData.equals(value);
    }

    // 데이터 삭제
    public void removeData(String type, String id) { redisTemplate.delete(buildKey(type, id)); }

    public void removeData(String key) { redisTemplate.delete(key); }

    // 데이터 반환 - Long 반환
    public Long getDataInLong(String type, String id){
        String value = redisTemplate.opsForValue().get(buildKey(type, id));

        if(value == null) return 0L;
        return Long.valueOf(value);
    }

    public Long getDataInLongWithNull(String type, String id){
        String value = redisTemplate.opsForValue().get(buildKey(type, id));

        if(value != null) return Long.valueOf(value);
        else { return null;}
    }

    // 데이터 반환 - String 반환
    public String getDataInStr(String type, String id){ return redisTemplate.opsForValue().get(buildKey(type, id)); }

    public Set<String> getMembersOfSet(String key) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        return setOps.members(key);
    }

    public List<String> getMembersOfList(String key) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        return listOps.range(key, 0, -1);
    }

    private String buildKey(String type, String id){
        return type + ":" + id;
    }

}