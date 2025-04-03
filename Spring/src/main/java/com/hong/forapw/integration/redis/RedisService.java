package com.hong.forapw.integration.redis;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    public void storeValue(String type, String id, String value, Long expirationTime) {
        redisTemplate.opsForValue().set(buildKey(type, id), value, expirationTime, TimeUnit.MILLISECONDS);
    }

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

    public void addSortedSetElement(String key, String member, double score) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        zSetOps.add(key, member, score);
    }

    public void removeSortedSetElement(String key, String member) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        zSetOps.remove(key, member);
    }

    // 특정 점수 범위 내에서 패턴으로 시작하는 Sorted Set 요소의 수를 계산
    public long countSortedSetElementsInRange(String key, String prefix, double minScore, double maxScore) {
        AtomicLong count = new AtomicLong(0);

        // 점수 범위 내의 모든 요소를 조회
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> elements =
                zSetOps.rangeByScoreWithScores(key, minScore, maxScore);

        if (elements != null) {
            elements.forEach(tuple -> {
                if (tuple.getValue() != null && tuple.getValue().startsWith(prefix)) {
                    count.incrementAndGet();
                }
            });
        }

        return count.get();
    }

    // 특정 프리픽스로 시작하는 Sorted Set 요소를 모두 제거
    public void removeSortedSetElementsByPrefix(String key, String prefix) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        Set<String> members = zSetOps.range(key, 0, -1);

        if (members != null) {
            members.stream()
                    .filter(member -> member.startsWith(prefix))
                    .forEach(member -> zSetOps.remove(key, member));
        }
    }

    public void addListElement(String key, String value, Long limit) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.leftPush(key, value);
        listOps.trim(key, 0, limit - 1);
    }

    public void incrementValue(String type, String id, Long value) {
        redisTemplate.opsForValue().increment(buildKey(type, id), value);
    }

    public Long incrementAndGet(String key, long increment) {
        return redisTemplate.opsForValue().increment(key, increment);
    }

    public Long incrementAndSetExpiration(String key, long increment, long expirationSeconds) {
        Long value = redisTemplate.opsForValue().increment(key, increment);
        redisTemplate.expire(key, expirationSeconds, TimeUnit.SECONDS);
        return value;
    }

    public void decrementValue(String type, String id, Long value) {
        redisTemplate.opsForValue().decrement(buildKey(type, id), value);
    }

    public void setKeyExpiration(String key, long expirationSeconds) {
        redisTemplate.expire(key, expirationSeconds, TimeUnit.SECONDS);
    }

    public void removeValue(String type, String id) {
        redisTemplate.delete(buildKey(type, id));
    }

    public void removeValue(String key) {
        redisTemplate.delete(key);
    }

    public void removeListElement(String key, String value) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.remove(key, 0, value);
    }

    public void removeSetElement(String key, String member) {
        redisTemplate.opsForSet().remove(key, member);
    }

    public boolean isValueStored(String type, String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(type, id)));
    }

    public boolean isValueNotStored(String type, String id) {
        return !Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(type, id)));
    }

    public boolean doesValueMismatch(String type, String id, String value) {
        String storedValue = redisTemplate.opsForValue().get(buildKey(type, id));
        return (storedValue == null) || !storedValue.equals(value);
    }

    public boolean isMemberOfSet(String key, String member) {
        Boolean result = redisTemplate.opsForSet().isMember(key, member);
        return Boolean.TRUE.equals(result);
    }

    public String getValueInString(String type, String id) {
        return redisTemplate.opsForValue().get(buildKey(type, id));
    }

    public Long getValueInLong(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(Long::valueOf)
                .orElse(0L);
    }

    public Long getValueInLong(String type, String id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(type, id)))
                .map(Long::valueOf)
                .orElse(0L);
    }

    public Long getValueInLongWithNull(String type, String id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(type, id)))
                .map(Long::valueOf)
                .orElse(null);
    }

    public Set<String> getMembersOfSet(String key) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        return setOps.members(key);
    }

    public String getHashValue(String type, String id, String field) {
        return (String) redisTemplate.opsForHash().get(buildKey(type, id), field);
    }

    public void setHashValue(String type, String id, String field, String value) {
        redisTemplate.opsForHash().put(buildKey(type, id), field, value);
    }

    public Map<Object, Object> getHashEntries(String type, String id) {
        return redisTemplate.opsForHash().entries(buildKey(type, id));
    }

    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    public void tryToAcquireLock(RLock lock, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.REDIS_LOCK_INTERRUPTED);
        }
    }

    public void safelyReleaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private String buildKey(String type, String id) {
        return type + ":" + id;
    }
}