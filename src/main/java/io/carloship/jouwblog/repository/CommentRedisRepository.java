package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.Application;
import io.carloship.jouwblog.response.Comment;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class CommentRedisRepository {

    private static final String PREFIX = "comments:";
    private static final String POST_COMMENT = "post_comments:";
    private static final Duration DEFAULT_EXPIRATION_SECONDS = Application.DEFAULT_REDIS_TIME;

    @Inject
    protected RedisAsyncCommands<String, String> asyncCommands;

    @Async
    @NonNull
    public CompletableFuture<List<Comment>> findComments(@NonNull String postId){
        if (postId.isBlank()){
            log.warn("Invalid comment argument for find operation, postId is blank");
            return CompletableFuture.completedFuture(List.of());
        }

        String key = buildKeyMembers(postId);

        return asyncCommands.smembers(key).thenCompose(commentsIds -> {
            if (commentsIds == null || commentsIds.isEmpty()){
                log.debug("No comments found for post: {}", postId);
                return CompletableFuture.completedFuture(List.of());
            }

            List<CompletableFuture<Comment>> commentsFeatures = commentsIds.stream()
                    .map(commentId -> findComment(postId, commentId))
                    .toList();

            return CompletableFuture.anyOf(commentsFeatures.toArray(new CompletableFuture[0]))
                    .thenApply(_ -> commentsFeatures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .exceptionally(ex -> {
                        log.error("Error while computing completable futures: {}", ex.getMessage());
                        return List.of();
                    });
        }).exceptionally(ex -> {
            log.error("Error finding all comments for postId: {}, error={}", postId, ex.getMessage(), ex);
            return List.of();
        }).toCompletableFuture();
    }

    @Async
    public CompletableFuture<Comment> findComment(@NonNull String postId, @NonNull String commentId){
        if (postId.isBlank() || commentId.isBlank()){
            log.warn("Invalid comment arguments for find operation");
            return CompletableFuture.completedFuture(null);
        }

        String key = buildKey(postId, commentId);
        return asyncCommands.hgetall(key)
                .thenApply(mappedResult -> {
                    if (mappedResult == null || mappedResult.isEmpty()) {
                        log.debug("Comment not found: commentId={}, postId={}", commentId, postId);
                        return null;
                    }

                    log.debug("Found comment: commentId={}, postId={}", commentId, postId);
                    Comment comment = new Comment();
                    return comment.fromMap(mappedResult);
                })
                .exceptionally(ex -> {
                    log.error("Error finding comment: commentId={}, postId={}, error={}",
                            commentId, postId, ex.getMessage(), ex);
                    return null;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> saveComment(@NonNull Comment comment){
        if ((comment.getPostId() == null || comment.getPostId().isBlank()) &&
                (comment.getCommentId() == null || comment.getCommentId().isBlank())){
            log.warn("Invalid comment data for save operation");
            return CompletableFuture.completedFuture(false);
        }

        var map = comment.toMap();
        String key = buildKey(comment.getPostId(),  comment.getCommentId());
        String memberKey  = buildKeyMembers(comment.getPostId());

        return asyncCommands.hset(key, map)
                .thenCompose(_ -> asyncCommands.expire(key, DEFAULT_EXPIRATION_SECONDS))
                .thenCompose(_ -> asyncCommands.sadd(memberKey, String.valueOf(comment.getPostId())))
                .thenCompose(_ -> asyncCommands.expire(memberKey, DEFAULT_EXPIRATION_SECONDS))
                .thenApply(_ -> {
                    log.debug("Comment saved successfully: commentId={}, postId={}", comment.getCommentId(), comment.getPostId());
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Error saving comment: commentId={}, postId={}, error={}", comment.getCommentId(), comment.getPostId(), ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> deleteComment(@NonNull String commentId, @NonNull String postId) {
        if (commentId.isBlank() || postId.isBlank()) {
            log.warn("Invalid comment argument for delete operation, postId or commentId is blank");
            return CompletableFuture.completedFuture(false);
        }

        String key = buildKey(commentId, postId);
        String membersKey = buildKeyMembers(postId);

        return asyncCommands.del(key)
                .thenCompose(_ -> asyncCommands.srem(membersKey, postId))
                .thenApply(_ -> {
                    log.debug("Comment deleted: commentId={}, postId={}", commentId, postId);
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Error deleting comment: commentId={}, postId={}, error={}", commentId, postId, ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    private String buildKey(String postId, String commentId){
        return PREFIX + postId + ":" + commentId;
    }

    private String buildKeyMembers(String postId){
        return POST_COMMENT + postId;
    }
}
