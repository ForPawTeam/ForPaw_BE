package com.hong.forapw.domain.search;

import com.hong.forapw.domain.meeting.model.query.GroupMeetingCountDTO;
import com.hong.forapw.domain.search.model.response.GroupDTO;
import com.hong.forapw.domain.search.model.response.PostDTO;
import com.hong.forapw.domain.search.model.response.SearchAllRes;
import com.hong.forapw.domain.search.model.response.ShelterDTO;
import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.shelter.Shelter;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.meeting.repository.MeetingRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.shelter.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final ShelterRepository shelterRepository;
    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final MeetingRepository meetingRepository;
    private final RedisService redisService;

    private static final String POST_LIKE_NUM_KEY_PREFIX = "postLikeNum";
    private static final Long POST_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달

    public SearchAllRes searchAll(String keyword, Pageable pageable) {
        List<ShelterDTO> shelterDTOS = searchShelterList(keyword, pageable);
        List<PostDTO> postDTOS = searchPostList(keyword, pageable);
        List<GroupDTO> groupDTOS = searchGroupList(keyword, pageable);

        return new SearchAllRes(shelterDTOS, postDTOS, groupDTOS);
    }

    public List<ShelterDTO> searchShelterList(String keyword, Pageable pageable) {
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<Shelter> shelters = shelterRepository.findByNameContaining(formattedKeyword, pageable).getContent();

        return shelters.stream()
                .map(shelter -> new ShelterDTO(shelter.getId(), shelter.getName()))
                .toList();
    }

    public List<PostDTO> searchPostList(String keyword, Pageable pageable) {
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<Object[]> posts = postRepository.findByTitleContaining(formattedKeyword, pageable).getContent();

        return posts.stream()
                .map(row -> {
                    Long likeNum = getCachedLikeNum(((Long) row[0]));
                    return new PostDTO(
                            (Long) row[0],  // postId
                            PostType.valueOf((String) row[4]),  // postType (String을 PostType으로 변환)
                            (String) row[1],  // title
                            (String) row[2],  // content
                            ((Timestamp) row[3]).toLocalDateTime(),  // createdDate (Timestamp를 LocalDateTime으로 변환)
                            (String) row[5],  // imageUrl
                            (String) row[7],   // nickName
                            (Long) row[8], // commentNum
                            likeNum);
                })
                .toList();
    }

    public List<GroupDTO> searchGroupList(String keyword, Pageable pageable) {
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<Group> groups = groupRepository.findByNameContaining(formattedKeyword, pageable).getContent();

        List<Long> groupIds = extractGroupIds(groups);
        Map<Long, Long> meetingCountByGroupId = getMeetingCountsByGroupIds(groupIds); // <groupId, meetingCount>

        return groups.stream()
                .map(group -> GroupDTO.fromEntity(group, meetingCountByGroupId))
                .toList();
    }

    private List<Long> extractGroupIds(List<Group> groups) {
        return groups.stream()
                .map(Group::getId)
                .toList();
    }

    private Map<Long, Long> getMeetingCountsByGroupIds(List<Long> groupIds) {
        return meetingRepository.countMeetingsByGroupIds(groupIds)
                .stream()
                .collect(Collectors.toMap(
                        GroupMeetingCountDTO::groupId,
                        GroupMeetingCountDTO::meetingCount
                ));
    }

    private String formatKeywordForFullTextSearch(String keyword) {
        String[] words = keyword.split("\\s+");
        StringBuilder modifiedKeyword = new StringBuilder();

        for (String word : words) {
            modifiedKeyword.append("+").append(word).append("* ").append(" ");
        }

        return modifiedKeyword.toString().trim();
    }

    private Long getCachedLikeNum(Long key) {
        Long likeNum = redisService.getValueInLongWithNull(SearchService.POST_LIKE_NUM_KEY_PREFIX, key.toString());

        if (likeNum == null) {
            likeNum = postRepository.countLikesByPostId(key);
            redisService.storeValue(SearchService.POST_LIKE_NUM_KEY_PREFIX, key.toString(), likeNum.toString(), POST_EXP);
        }

        return likeNum;
    }
}