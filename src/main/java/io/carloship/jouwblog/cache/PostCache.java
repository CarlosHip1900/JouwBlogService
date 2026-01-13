package io.carloship.jouwblog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.carloship.jouwblog.repository.PostRedisRepository;
import io.carloship.jouwblog.response.Post;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Singleton
public class PostCache {

    private final Cache<String, CopyOnWriteArrayList<String>> userPostsCache;
    private final Cache<String, Post> postCache;
    private final PostRedisRepository redisRepository;

    public PostCache(PostRedisRepository redisRepository) {
        this.redisRepository = redisRepository;

        // Using thread-safe CopyOnWriteArrayList for concurrent modifications
        this.userPostsCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(20)) // Slightly longer than posts
                .maximumSize(10_000) // Prevent unbounded growth
                .build();

        this.postCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .maximumSize(100_000) // Prevent unbounded growth
                .removalListener(this::onPostRemoval)
                .build();
    }

    private void onPostRemoval(String key, Post value, RemovalCause cause) {
        if (key == null || value == null) return;

        if (cause == RemovalCause.EXPLICIT) return;

        var userPosts = userPostsCache.getIfPresent(value.getUserId());
        if (userPosts != null) {
            userPosts.remove(value.getPostId());
        }

        saveToRedis(value);
    }

    private void saveToRedis(Post post) {
        redisRepository.savePost(post)
                .thenAccept(result -> {
                    if (result) {
                        log.debug("Post {} saved to Redis", post.getPostId());
                    } else {
                        log.warn("Post {} could not be saved to Redis", post.getPostId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error saving post {} to Redis: {}", post.getPostId(), ex.getMessage(), ex);
                    return null;
                });
    }

    protected List<String> getUserPostIds(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return userPostsCache.get(userId, _ -> new CopyOnWriteArrayList<>());
    }

    public List<Post> getUserPosts(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        var postIds = getUserPostIds(userId);
        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Post> posts = new ArrayList<>(postIds.size());
        for (String id : postIds) {
            var post = postCache.getIfPresent(id);
            if (post != null) {
                posts.add(post);
            }
        }

        return posts;
    }

    public void addPost(Post post) {
        Objects.requireNonNull(post, "post cannot be null");
        Objects.requireNonNull(post.getPostId(), "postId cannot be null");
        Objects.requireNonNull(post.getUserId(), "userId cannot be null");

        postCache.put(post.getPostId(), post);

        var userPosts = getUserPostIds(post.getUserId());

        // CopyOnWriteArrayList handles concurrency, but check for duplicates
        if (!userPosts.contains(post.getPostId())) {
            userPosts.add(post.getPostId());
        }
    }

    public Post getPost(String postId) {
        Objects.requireNonNull(postId, "postId cannot be null");
        return postCache.getIfPresent(postId);
    }

    public void removePost(String postId) {
        Objects.requireNonNull(postId, "postId cannot be null");

        var post = postCache.getIfPresent(postId);
        if (post != null) {
            postCache.invalidate(postId);

            var userPosts = userPostsCache.getIfPresent(post.getUserId());
            if (userPosts != null) {
                userPosts.remove(postId);
            }
        }
    }

    public void invalidateUser(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        userPostsCache.invalidate(userId);
    }
}