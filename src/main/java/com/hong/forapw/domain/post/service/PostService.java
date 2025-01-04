package com.hong.forapw.domain.post.service;

import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.PostValidator;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.PopularPost;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.entity.PostImage;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.entity.Report;
import com.hong.forapw.domain.post.model.request.*;
import com.hong.forapw.domain.post.model.request.PostImageDTO;
import com.hong.forapw.domain.post.model.response.*;
import com.hong.forapw.domain.post.repository.*;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.admin.repository.ReportRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.hong.forapw.common.constants.GlobalConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReportRepository reportRepository;
    private final PopularPostRepository popularPostRepository;
    private final UserRepository userRepository;
    private final PostCacheService postCacheService;
    private final LikeService likeService;
    private final AlarmService alarmService;
    private final PostValidator validator;

    private static final String POST_SCREENED = "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.";
    private static final String COMMENT_DELETED = "삭제된 댓글 입니다.";

    @Transactional
    public CreatePostRes createPost(CreatePostReq request, Long userId) {
        validator.validatePostRequest(request);

        List<PostImage> postImages = PostImageDTO.fromDTOs(request.images());

        User writer = userRepository.getReferenceById(userId);
        Post post = request.toEntity(writer);
        post.setPostRelationships(postImages);

        postRepository.save(post);
        postCacheService.initializePostCache(post.getId());

        return new CreatePostRes(post.getId());
    }

    @Transactional
    public CreateAnswerRes createAnswer(CreateAnswerReq request, Long questionPostId, Long userId) {
        Post question = postRepository.findByIdWithUser(questionPostId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        question.validateQuestionType();

        List<PostImage> answerImages = PostImageDTO.fromDTOs(request.images());

        User writer = userRepository.getReferenceById(userId);
        Post answer = request.toEntity(writer, question);
        answer.setAnswerRelationships(answerImages, question);

        postRepository.save(answer);

        question.incrementAnswerNum();
        sendNewAnswerAlarm(question, request.content(), questionPostId);

        return new CreateAnswerRes(answer.getId());
    }

    public FindPostListRes findPostsByType(Pageable pageable, PostType postType) {
        Page<Post> postPage = postRepository.findByPostTypeWithUser(postType, pageable);
        List<Long> postIds = postPage.getContent().stream()
                .map(Post::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(postIds, Like.POST);
        List<FindPostListRes.PostDTO> postDTOs = FindPostListRes.PostDTO.fromEntities(postPage.getContent(), likeCountMap);

        return new FindPostListRes(postDTOs, postPage.isLast());
    }

    public FindPostListRes findPopularPostsByType(Pageable pageable, PostType postType) {
        Page<PopularPost> popularPostPage = popularPostRepository.findByPostTypeWithPost(postType, pageable);
        Map<Long, Long> likeCountMap = likeService.getLikeCounts(
                popularPostPage.stream().map(PopularPost::getPostId).toList(), Like.POST
        );

        List<FindPostListRes.PostDTO> postDTOs = FindPostListRes.PostDTO.fromEntities(
                popularPostPage.stream().map(PopularPost::getPost).toList(), likeCountMap
        );

        return new FindPostListRes(postDTOs, popularPostPage.isLast());
    }

    public FindQnaListRes findQuestions(Pageable pageable) {
        Page<Post> questionPage = postRepository.findByPostTypeWithUser(PostType.QUESTION, pageable);
        return new FindQnaListRes(questionPage);
    }

    public FindMyPostListRes findMyPosts(Long userId, Pageable pageable) {
        Page<Post> postPage = postRepository.findPostsByUserIdAndTypesWithUser(userId, MY_POST_TYPES, pageable);
        List<Post> posts = postPage.getContent();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(posts.stream().map(Post::getId).toList(), Like.POST);

        List<FindMyPostListRes.MyPostDTO> postDTOs = FindMyPostListRes.MyPostDTO.fromEntities(posts, likeCountMap);
        return new FindMyPostListRes(postDTOs, postPage.isLast());
    }

    public FindQnaListRes findMyQuestions(Long userId, Pageable pageable) {
        Page<Post> questionPage = postRepository.findPostsByUserIdAndTypesWithUser(userId, QUESTION_TYPES, pageable);
        return new FindQnaListRes(questionPage);
    }

    public FindQnaListRes findQuestionsAnsweredByMe(Long userId, Pageable pageable) {
        Page<Post> questionPage = postRepository.findQnaOfAnswerByUserIdWithUser(userId, pageable);
        return new FindQnaListRes(questionPage);
    }

    public FindMyCommentListRes findMyComments(Long userId, Pageable pageable) {
        Page<Comment> myCommentPage = commentRepository.findByUserIdWithPost(userId, pageable);
        return new FindMyCommentListRes(myCommentPage);
    }

    public FindPostByIdRes findPostById(Long postId, Long userId) {
        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        post.validatePostState();

        Long likeCount = likeService.getLikeCount(postId, Like.POST);
        List<FindPostByIdRes.CommentDTO> commentDTOs = buildCommentDTOs(postId, userId);

        postCacheService.incrementPostViewCount(postId);
        postCacheService.markNoticePostAsRead(post, userId, postId);

        return new FindPostByIdRes(post, commentDTOs, userId, likeCount, isPostLiked(postId, userId));
    }

    public FindQnaByIdRes findQnaById(Long qnaId, Long userId) {
        Post qna = postRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        qna.validateQnaState();

        List<Post> answers = postRepository.findByParentIdWithUser(qnaId);

        postCacheService.incrementPostViewCount(qnaId);
        return new FindQnaByIdRes(qna, answers, userId);
    }

    public FindAnswerByIdRes findAnswerById(Long postId, Long userId) {
        Post answer = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        answer.validateAnswerType();

        return new FindAnswerByIdRes(answer, userId);
    }

    @Transactional
    public void updatePost(UpdatePostReq request, User user, Long postId) {
        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        validator.validateAccessorAuthorization(user, post.getWriterId());

        post.updateContent(request.title(), request.content());

        removeUnretainedImages(request.retainedImageIds(), postId);
        saveNewPostImages(request.newImages(), post);
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        Post post = postRepository.findByIdWithUserAndParent(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        validator.validateAccessorAuthorization(user, post.getWriterId());

        postLikeRepository.deleteAllByPostId(postId);
        commentLikeRepository.deleteByPostId(postId);
        popularPostRepository.deleteByPostId(postId);
        postImageRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId); // soft-delete
        postRepository.delete(post); // soft-delete
    }

    @Transactional
    public void deleteAnswer(Long answerId, User user) {
        Post answer = postRepository.findByIdWithUserAndParent(answerId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        answer.validateAnswerType();
        validator.validateAccessorAuthorization(user, answer.getWriterId());

        decrementAnswerCount(answer);
        postImageRepository.deleteByPostId(answerId);
        postRepository.deleteById(answerId);
    }

    @Transactional
    public CreateCommentRes createComment(CreateCommentReq request, Long userId, Long postId) {
        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));

        post.validatePostState();

        User writer = userRepository.getReferenceById(userId);
        Comment comment = request.toEntity(request.content(), post, writer);

        commentRepository.save(comment);

        postRepository.incrementCommentCount(postId);
        postCacheService.initializeCommentCache(comment.getId());
        notifyNewComment(request.content(), postId, post.getPostType(), post.getWriterId());

        return new CreateCommentRes(comment.getId());
    }

    @Transactional
    public CreateCommentRes createReply(CreateCommentReq request, Long postId, Long userId, Long parentCommentId) {
        Comment parentComment = commentRepository.findByIdWithPost(parentCommentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));

        parentComment.validateParentCommentState(postId);

        Post post = postRepository.getReferenceById(postId);
        User writer = userRepository.getReferenceById(userId);
        Comment reply = request.toEntity(request.content(), post, writer);
        parentComment.addChildComment(reply);

        commentRepository.save(reply);

        postRepository.incrementCommentCount(postId);
        postCacheService.initializeCommentCache(reply.getId());
        notifyNewReply(request.content(), postId, parentComment);

        return new CreateCommentRes(reply.getId());
    }

    @Transactional
    public void updateComment(UpdateCommentReq request, Long postId, Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));

        comment.validateCommentBelongsToPost(postId);
        validator.validateAccessorAuthorization(user, comment.getWriterId());

        comment.updateContent(request.content());
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));

        comment.validateCommentBelongsToPost(postId);
        validator.validateAccessorAuthorization(user, comment.getWriterId());

        comment.updateContent(COMMENT_DELETED);
        commentRepository.deleteById(commentId);
        commentLikeRepository.deleteAllByCommentId(commentId);

        adjustCommentCountOnDeletion(comment, postId);
    }

    @Transactional
    public void submitReport(SubmitReportReq request, Long reporterId) {
        validator.validateReportRequest(request.contentId(), request.contentType(), reporterId);

        User reportedUser = getOffendingUser(request);
        validator.validateNotSelfReport(reporterId, reportedUser);

        User reporter = userRepository.getReferenceById(reporterId);
        Report report = request.toEntity(reporter, reportedUser);

        reportRepository.save(report);
    }

    @Transactional
    public void refreshPopularPostsWithinRange(LocalDateTime start, LocalDateTime end, PostType postType) {
        List<Post> posts = postRepository.findByDateAndType(start, end, postType);
        processPopularPosts(posts, postType);
    }

    private void sendNewAnswerAlarm(Post questionPost, String answerContent, Long questionPostId) {
        String content = "새로운 답변: " + answerContent;
        String redirectURL = "/community/question/" + questionPostId;

        alarmService.sendAlarm(questionPost.getUser().getId(), content, redirectURL, AlarmType.ANSWER);
    }

    private boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    private void removeUnretainedImages(List<Long> retainedImageIds, Long postId) {
        if (retainedImageIds != null && !retainedImageIds.isEmpty()) {
            postImageRepository.deleteByPostIdAndIdNotIn(postId, retainedImageIds);
        } else {
            postImageRepository.deleteByPostId(postId);
        }
    }

    private void saveNewPostImages(List<PostImageDTO> newImageDTOs, Post post) {
        List<PostImage> newPostImages = PostImageDTO.fromDTDs(newImageDTOs, post);
        postImageRepository.saveAll(newPostImages);
    }

    private void decrementAnswerCount(Post answer) {
        Post question = answer.getParent();
        if (question != null) {
            postRepository.decrementAnswerNum(question.getId());
        }
    }

    private void notifyNewComment(String commentContent, Long postId, PostType postType, Long writerId) {
        String content = "새로운 댓글: " + commentContent;
        String queryParam = postType.name().toLowerCase();
        String redirectURL = "/community/" + postId + "?type=" + queryParam;
        alarmService.sendAlarm(writerId, content, redirectURL, AlarmType.COMMENT);
    }

    private void notifyNewReply(String replyContent, Long postId, Comment parentComment) {
        String content = "새로운 대댓글: " + replyContent;
        String queryParam = parentComment.getPostTypeName().toLowerCase();
        String redirectURL = "/community/" + postId + "?type=" + queryParam;
        alarmService.sendAlarm(parentComment.getWriterId(), content, redirectURL, AlarmType.COMMENT);
    }

    private void adjustCommentCountOnDeletion(Comment comment, Long postId) {
        long replyCount = comment.getReplyCount();
        postRepository.decrementCommentNum(postId, 1L + replyCount);
    }

    private User getOffendingUser(SubmitReportReq request) {
        if (request.contentType() == ContentType.POST) {
            return postRepository.findUserById(request.contentId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));
        }

        if (request.contentType() == ContentType.COMMENT) {
            return commentRepository.findUserById(request.contentId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));
        }

        throw new CustomException(ExceptionCode.INVALID_REPORT_TARGET);
    }

    private void processPopularPosts(List<Post> posts, PostType postType) {
        posts.forEach(this::updatePostHotPoint);

        List<Post> popularPosts = selectPopularPosts(posts);
        fillPopularPostsIfNecessary(posts, popularPosts);
        savePopularPosts(popularPosts, postType);
    }

    private void updatePostHotPoint(Post post) {
        double hotPoint = calculateHotPoint(post);
        post.updateHotPoint(hotPoint);
    }

    private double calculateHotPoint(Post post) {
        double viewPoints = postCacheService.getPostViewCount(post.getId(), post) * 0.001;
        double commentPoints = post.getCommentNum();
        double likePoints = likeService.getLikeCount(post.getId(), Like.POST) * 5.0;
        return viewPoints + commentPoints + likePoints;
    }

    private List<Post> selectPopularPosts(List<Post> posts) {
        return posts.stream()
                .filter(post -> post.getHotPoint() > 10.0)
                .toList();
    }

    private void fillPopularPostsIfNecessary(List<Post> allPosts, List<Post> popularPosts) {
        if (popularPosts.size() >= 5) return;
        List<Post> remainingPosts = allPosts.stream()
                .filter(post -> !popularPosts.contains(post))
                .sorted(Comparator.comparingDouble(Post::getHotPoint).reversed())
                .toList();

        popularPosts.addAll(remainingPosts.stream()
                .limit(5L - popularPosts.size())
                .toList());
    }

    private void savePopularPosts(List<Post> popularPosts, PostType postType) {
        popularPosts.forEach(post -> {
            PopularPost popularPost = PopularPost.builder()
                    .post(post)
                    .postType(postType)
                    .build();

            popularPostRepository.save(popularPost);
        });
    }

    private List<FindPostByIdRes.CommentDTO> buildCommentDTOs(Long postId, Long userId) {
        List<Comment> comments = commentRepository.findByPostIdWithUserAndParentAndRemoved(postId);
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(commentIds, Like.COMMENT);
        List<Long> likedCommentIds = commentLikeRepository.findCommentIdsByUserId(userId);

        Map<Long, FindPostByIdRes.CommentDTO> parentCommentMap = new HashMap<>();
        comments.forEach(comment -> {
            if (comment.isParent()) {
                addParentToMap(comment, parentCommentMap, likeCountMap.getOrDefault(comment.getId(), DEFAULT_VALUE), likedCommentIds.contains(comment.getId()));
            } else {
                addReplyToParent(comment, parentCommentMap, likeCountMap.getOrDefault(comment.getId(), DEFAULT_VALUE), likedCommentIds.contains(comment.getId()));
            }
        });
        return new ArrayList<>(parentCommentMap.values());
    }

    private void addParentToMap(Comment parent, Map<Long, FindPostByIdRes.CommentDTO> parentCommentMap, Long likeCount, boolean isLiked) {
        FindPostByIdRes.CommentDTO parentCommentDTO = new FindPostByIdRes.CommentDTO(parent, likeCount, isLiked);
        parentCommentMap.put(parent.getId(), parentCommentDTO);
    }

    private void addReplyToParent(Comment reply, Map<Long, FindPostByIdRes.CommentDTO> parentCommentMap, Long likeCount, boolean isLiked) {
        FindPostByIdRes.CommentDTO parentCommentDTO = parentCommentMap.get(reply.getParentId());
        FindPostByIdRes.ReplyDTO replyDTO = new FindPostByIdRes.ReplyDTO(reply, isLiked, likeCount);
        parentCommentDTO.replies().add(replyDTO);
    }
}