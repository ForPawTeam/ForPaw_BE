package com.hong.forapw.domain.search;

import com.hong.forapw.domain.like.LikeService;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.meeting.model.query.GroupMeetingCountDTO;
import com.hong.forapw.domain.post.model.query.PostProjection;
import com.hong.forapw.domain.search.model.response.GroupDTO;
import com.hong.forapw.domain.search.model.response.PostDTO;
import com.hong.forapw.domain.search.model.response.SearchAllRes;
import com.hong.forapw.domain.search.model.response.ShelterDTO;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.shelter.Shelter;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.meeting.repository.MeetingRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.shelter.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LikeService likeService;

    public SearchAllRes searchAll(String keyword, Pageable pageable) {
        List<ShelterDTO> shelterDTOs = searchShelters(keyword, pageable);
        List<PostDTO> postDTOs = searchPosts(keyword, pageable);
        List<GroupDTO> groupDTOs = searchGroups(keyword, pageable);

        return new SearchAllRes(shelterDTOs, postDTOs, groupDTOs);
    }

    public List<ShelterDTO> searchShelters(String keyword, Pageable pageable) {
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<Shelter> shelters = shelterRepository.findByNameContaining(formattedKeyword, pageable).getContent();

        return ShelterDTO.fromEntities(shelters);
    }

    public List<PostDTO> searchPosts(String keyword, Pageable pageable) {
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<PostProjection> queryResults = postRepository.findByTitleContaining(formattedKeyword, pageable).getContent();
        List<Long> postIds = queryResults.stream().map(PostProjection::getPostId).toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(postIds, Like.POST);
        return PostDTO.fromQureryResults(queryResults, likeCountMap);
    }

    public List<GroupDTO> searchGroups(String keyword, Pageable pageable) {
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<Group> groups = groupRepository.findByNameContaining(formattedKeyword, pageable).getContent();
        List<Long> groupIds = groups.stream().map(Group::getId).toList();

        Map<Long, Long> meetingCountMap = getMeetingCountMap(groupIds);
        return GroupDTO.fromEntities(groups, meetingCountMap);
    }

    private Map<Long, Long> getMeetingCountMap(List<Long> groupIds) {
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
}