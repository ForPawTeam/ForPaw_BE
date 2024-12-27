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

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;
    private static final String SORT_BY_ID = "id";

    @GetMapping("/search/all")
    public ResponseEntity<?> searchAll(@RequestParam @NotEmpty String keyword,
                                       @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        SearchAllRes response = searchService.searchAll(keyword, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/search/shelters")
    public ResponseEntity<?> searchShelterList(@RequestParam @NotEmpty String keyword,
                                               @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<ShelterDTO> shelterDTOS = searchService.searchShelterList(keyword, pageable);
        SearchShelterListRes response = new SearchShelterListRes(shelterDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/search/posts")
    public ResponseEntity<?> searchPostList(@RequestParam @NotEmpty String keyword,
                                            @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<PostDTO> postDTOS = searchService.searchPostList(keyword, pageable);
        SearchPostListRes response = new SearchPostListRes(postDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/search/groups")
    public ResponseEntity<?> searchGroupList(@RequestParam @NotEmpty String keyword,
                                             @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<GroupDTO> groupDTOS = searchService.searchGroupList(keyword, pageable);
        SearchGroupListRes response = new SearchGroupListRes(groupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}
