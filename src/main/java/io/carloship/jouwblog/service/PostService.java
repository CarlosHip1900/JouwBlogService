package io.carloship.jouwblog.service;

import io.carloship.jouwblog.cache.PostCache;
import io.carloship.jouwblog.common.Utils;
import io.carloship.jouwblog.repository.PostRedisRepository;
import io.carloship.jouwblog.repository.PostRepository;
import io.carloship.jouwblog.response.Post;
import io.micronaut.data.model.Pageable;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class PostService {

    @Inject
    protected PostRepository repository;

    @Inject
    protected PostRedisRepository redisRepository;

    @Inject
    protected PostCache postCache;

    @Async
    public CompletableFuture<Post> findPost(@NonNull String postId, @NonNull String userId){
        var cached = postCache.getPost(postId);
        if  (cached != null) { return CompletableFuture.completedFuture(cached); }

        return redisRepository.findPost(userId, postId).thenCompose(redisPost -> {
            if (redisPost != null) {
                postCache.addPost(redisPost);
                return CompletableFuture.completedFuture(redisPost);
            }

            return repository.findByUserId(userId, postId).thenApply(mongoPost -> {
                if (mongoPost != null){
                    postCache.addPost(mongoPost);
                    redisRepository.savePost(mongoPost);
                    return mongoPost;
                }

                return null;
            }).exceptionally(ex -> {
                log.error("Error while find post {} in mongo from user {}: {}", postId, userId, ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            log.error("Error while find post {} in redis from user {}: {}", postId, userId, ex.getMessage());
            return null;
        });
    }

    @Async
    public CompletableFuture<List<Post>> findAllPage(@NonNull String userId, int size, int page) {
        List<Post> cached = postCache.getUserPosts(userId);
        int fromIndex = page * size;

        if (cached != null && cached.size() > fromIndex) {
            return CompletableFuture.completedFuture(Utils.getPage(cached, page, size));
        }

        return redisRepository.findAllUserPosts(userId).thenCompose(redisPosts -> {
            if (redisPosts != null && redisPosts.size() > fromIndex) {

                redisPosts.forEach(postCache::addPost);
                return CompletableFuture.completedFuture(Utils.getPage(redisPosts, page, size));
            }

            return repository.findPostsByUserId(userId, Pageable.from(page, size))
                    .thenApply(result -> {
                        List<Post> content = result.getContent();

                        content.forEach(post -> {
                            postCache.addPost(post);
                            redisRepository.savePost(post);
                        });

                        return content;
                    })
                    .exceptionally(ex -> {
                        log.error("Error while process user {} posts by pagination in mongo: {}", userId, ex.getMessage());
                        return null;
                    });
        }).exceptionally(ex -> {
            log.error("Error while process user {} posts by pagination in redis: {}", userId, ex.getMessage());
            return null;
        });
    }

    @Async
    public CompletableFuture<Post> savePost(@NonNull Post post){
        post.setPostTimestamp(System.currentTimeMillis());
        if (post.getPostId() == null){
            return repository.save(post).thenCompose(postWithId -> {
                if (postWithId.getPostId() == null || postWithId.getPostId().isBlank()){
                    log.warn("Post id isn't created in mongo... back end problem...");
                    return CompletableFuture.completedFuture(null);
                }

                return redisRepository.savePost(postWithId).thenApply(operation -> {
                    if (!operation){
                        log.warn("Cannot possible add post in redis...");
                        return null;
                    }

                    postCache.addPost(postWithId);
                    return postWithId;
                });
            });
        }

        return repository.update(post).thenCompose(updated -> {
            log.debug("Updating post {} in redis...", updated.getPostId());
            return redisRepository.savePost(updated).thenApply(operation -> {
                if (!operation){
                    log.warn("Cannot possible update post in redis...");
                    return null;
                }

                postCache.addPost(updated);
                return updated;
            });
        });
    }

    @Async
    public CompletableFuture<Void> deletePost(@NonNull String postId, @NonNull String userId){
        return repository.deleteById(postId).thenCompose(_1 ->
                redisRepository.deletePost(userId, postId).thenAccept(_2 ->
                        postCache.removePost(postId))
                        .exceptionally(ex -> {
                            log.error("Error while delete post {} from user {} in redis: {}", postId, userId, ex.getMessage());
                            return null;
                        })
        ).exceptionally(ex -> {
            log.error("Error while delete post {} from user {} in mongo: {}", postId, userId, ex.getMessage());
            return null;
        });
    }
}
