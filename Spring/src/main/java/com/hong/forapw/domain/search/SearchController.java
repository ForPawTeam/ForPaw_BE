package com.hong.forapw.domain.search;

import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.search.model.response.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_ID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search/all")
    public ResponseEntity<?> searchAll(@RequestParam @NotEmpty String keyword,
                                       @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        SearchAllRes response = searchService.searchAll(keyword, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/search/shelters")
    public ResponseEntity<?> searchShelters(@RequestParam @NotEmpty String keyword,
                                            @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<ShelterDTO> shelterDTOs = searchService.searchShelters(keyword, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new SearchShelterListRes(shelterDTOs)));
    }

    @GetMapping("/search/posts")
    public ResponseEntity<?> searchPosts(@RequestParam @NotEmpty String keyword,
                                         @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<PostDTO> postDTOs = searchService.searchPosts(keyword, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new SearchPostListRes(postDTOs)));
    }

    @GetMapping("/search/groups")
    public ResponseEntity<?> searchGroups(@RequestParam @NotEmpty String keyword,
                                          @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<GroupDTO> groupDTOs = searchService.searchGroups(keyword, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new SearchGroupListRes(groupDTOs)));
    }
}
