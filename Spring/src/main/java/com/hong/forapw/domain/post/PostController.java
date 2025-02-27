package com.hong.forapw.domain.post;

import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.model.request.*;
import com.hong.forapw.domain.post.model.response.*;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.service.PostService;
import com.hong.forapw.domain.like.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_ID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final LikeService likeService;

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody @Valid CreatePostReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreatePostRes response = postService.createPost(request, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/posts/{postId}/qna")
    public ResponseEntity<?> createAnswer(@RequestBody @Valid CreateAnswerReq request, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreateAnswerRes response = postService.createAnswer(request, postId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/adoption")
    public ResponseEntity<?> findAdoptionPostList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        FindPostListRes response = postService.findPostsByType(pageable, PostType.ADOPTION);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/fostering")
    public ResponseEntity<?> findFosteringPostList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        FindPostListRes response = postService.findPostsByType(pageable, PostType.FOSTERING);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/question")
    public ResponseEntity<?> findQuestionList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        FindQnaListRes response = postService.findQuestions(pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/popular")
    public ResponseEntity<?> findPopularPostList(@RequestParam PostType type,
                                                 @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        FindPostListRes response = postService.findPopularPostsByType(pageable, type);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/myPost")
    public ResponseEntity<?> findMyPosts(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindMyPostListRes response = postService.findMyPosts(userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/myQuestion")
    public ResponseEntity<?> findMyQuestions(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindQnaListRes response = postService.findMyQuestions(userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/myAnswer")
    public ResponseEntity<?> findQuestionsAnsweredByMe(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindQnaListRes response = postService.findQuestionsAnsweredByMe(userDetails.getUserId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/myComment")
    public ResponseEntity<?> findMyCommentList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindMyCommentListRes response = postService.findMyComments(userDetails.user().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> findPostById(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindPostByIdRes response = postService.findPostById(postId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/posts/{postId}/qna")
    public ResponseEntity<?> findQnaById(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindQnaByIdRes response = postService.findQnaById(postId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/answers/{answerId}")
    public ResponseEntity<?> findAnswerById(@PathVariable Long answerId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindAnswerByIdRes response = postService.findAnswerById(answerId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<?> updatePost(@RequestBody @Valid UpdatePostReq request, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.updatePost(request, userDetails.user(), postId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.user());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<?> deleteAnswer(@PathVariable Long answerId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deleteAnswer(answerId, userDetails.user());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        likeService.like(postId, userDetails.getUserId(), Like.POST);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(@RequestBody @Valid CreateCommentReq request, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreateCommentRes response = postService.createComment(request, userDetails.getUserId(), postId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/reply")
    public ResponseEntity<?> createReply(@RequestBody @Valid CreateCommentReq request, @PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreateCommentRes response = postService.createReply(request, postId, userDetails.getUserId(), commentId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> updateComment(@RequestBody @Valid UpdateCommentReq request, @PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.updateComment(request, postId, commentId, userDetails.user());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deleteComment(postId, commentId, userDetails.user());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(@PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        likeService.like(commentId, userDetails.getUserId(), Like.COMMENT);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/reports")
    public ResponseEntity<?> submitReport(@RequestBody @Valid SubmitReportReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.submitReport(request, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}