package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.Post.Type;
import com.hong.ForPaw.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody PostRequest.CreatePostDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){

        PostResponse.CreatePostDTO responseDTO = postService.createPost(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts")
    public ResponseEntity<?> findPostList(@RequestParam Type type, Pageable pageable){

        PostResponse.FindPostListDTO responseDTO = postService.findPostList(type, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> findPostById(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails){

        PostResponse.FindPostByIdDTO responseDTO = postService.findPostById(postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<?> updatePost(@RequestBody PostRequest.UpdatePostDTO requestDTO, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails){

        postService.updatePost(requestDTO, userDetails.getUser().getId(), postId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails){

        postService.deletePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails){

        postService.likePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(@RequestBody PostRequest.CreateCommentDTO requestDTO, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails){

        PostResponse.CreateCommentDTO responseDTO = postService.createComment(requestDTO, userDetails.getUser().getId(), postId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<?> updateComment(@RequestBody PostRequest.UpdateCommentDTO requestDTO, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails){

        postService.updateComment(requestDTO, commentId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(@PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails){

        postService.likeComment(commentId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}