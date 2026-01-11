package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.Application;
import io.carloship.jouwblog.response.Post;
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
public class PostRedisRepository {

    private static final String PREFIX = "post:";
    private static final String USER_POSTS_SET_PREFIX = "user_posts:";
    private static final Duration DEFAULT_EXPIRATION_SECONDS = Application.DEFAULT_REDIS_TIME;

    @Inject
    protected RedisAsyncCommands<String, String> asyncCommands;

    @Async
    public CompletableFuture<Post> findPost(@NonNull String userId,@NonNull String postId) {
        if (userId.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        String key = buildPostKey(userId, postId);
        return asyncCommands.hgetall(key)
                .thenApply(mappedResult -> {
                    if (mappedResult == null || mappedResult.isEmpty()) {
                        log.debug("Post not found: userId={}, postId={}", userId, postId);
                        return null;
                    }

                    log.debug("Found post: userId={}, postId={}", userId, postId);
                    Post post = new Post();
                    return post.fromMap(mappedResult);
                })
                .exceptionally(ex -> {
                    log.error("Error finding post: userId={}, postId={}, error={}",
                            userId, postId, ex.getMessage(), ex);
                    return null;
                })
                .toCompletableFuture();
    }

    @Async
    @NonNull
    public CompletableFuture<List<Post>> findAllUserPosts(@NonNull String userId) {
        if (userId.isBlank()) {
            log.warn("Invalid user arguments for find posts operation, userIs is blank!");
            return CompletableFuture.completedFuture(List.of());
        }

        String userPostsKey = buildUserPostsSetKey(userId);

        return asyncCommands.smembers(userPostsKey)
                .thenCompose(postIds -> {
                    if (postIds == null || postIds.isEmpty()) {
                        log.debug("No posts found for user: {}", userId);
                        return CompletableFuture.completedFuture(List.of());
                    }

                    List<CompletableFuture<Post>> postFutures = postIds.stream()
                            .map(postIdStr -> findPost(userId, postIdStr))
                            .toList();

                    return CompletableFuture.allOf(postFutures.toArray(new CompletableFuture[0]))
                            .thenApply(_ -> postFutures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()))
                            .exceptionally(ex -> {
                                log.error("Error while computing completable futures: {}", ex.getMessage());
                                return List.of();
                            });
                }).exceptionally(ex -> {
                    log.error("Error finding all posts for user: {}, error={}", userId, ex.getMessage(), ex);
                    return List.of();
                }).toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> savePost(@NonNull Post post) {
        if (post.getUserId() == null || post.getUserId().isBlank()) {
            log.warn("Invalid post data for save operation");
            return CompletableFuture.completedFuture(false);
        }

        var map = post.toMap();
        String key = buildPostKey(post.getUserId(), post.getPostId());
        String userPostsKey = buildUserPostsSetKey(post.getUserId());

        return asyncCommands.hset(key, map)
                .thenCompose(_ -> asyncCommands.expire(key, DEFAULT_EXPIRATION_SECONDS))
                .thenCompose(_ -> asyncCommands.sadd(userPostsKey, String.valueOf(post.getPostId())))
                .thenCompose(_ -> asyncCommands.expire(userPostsKey, DEFAULT_EXPIRATION_SECONDS))
                .thenApply(_ -> {
                    log.debug("Post saved successfully: userId={}, postId={}", post.getUserId(), post.getPostId());
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Error saving post: userId={}, postId={}, error={}", post.getUserId(), post.getPostId(), ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> deletePost(@NonNull String userId, @NonNull String postId) {
        if (userId.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String key = buildPostKey(userId, postId);
        String userPostsKey = buildUserPostsSetKey(userId);

        return asyncCommands.del(key)
                .thenCompose(_ -> asyncCommands.srem(userPostsKey, postId))
                .thenApply(_ -> {
                    log.debug("Post deleted: userId={}, postId={}", userId, postId);
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Error deleting post: userId={}, postId={}, error={}",
                            userId, postId, ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    private String buildPostKey(String userId, String postId) {
        return PREFIX + userId + ":" + postId;
    }

    private String buildUserPostsSetKey(String userId) {
        return USER_POSTS_SET_PREFIX + userId;
    }
}