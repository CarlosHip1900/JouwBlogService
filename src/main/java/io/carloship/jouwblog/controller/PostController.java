package io.carloship.jouwblog.controller;

import io.carloship.jouwblog.response.Post;
import io.carloship.jouwblog.service.PostService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@ExecuteOn(TaskExecutors.IO)
@Controller("/posts/")
public class PostController {

    @Inject
    protected PostService service;

    @Async
    @Get("/all/{userId}/{size}/{page}")
    CompletableFuture<List<Post>> findAllPosts(@NonNull @PathVariable String userId, @PathVariable int size, @PathVariable int page){
        if (userId.isBlank()){
            return CompletableFuture.completedFuture(List.of());
        }

        return service.findAllPage(userId, size, page).exceptionally(ex -> {
                    log.error("Error while find all posts from user {}: {}", userId, ex.getMessage());
                    return List.of();
                });
    }

    @Async
    @Get("/single/{userId}/{postId}")
    CompletableFuture<Post> findPost(@NonNull @PathVariable String userId, @NonNull @PathVariable String postId){
        if (userId.isBlank() || postId.isBlank()){
            return CompletableFuture.completedFuture(null);
        }

        return service.findPost(postId, userId).exceptionally(ex -> {
            log.error("Error while find post {} from user {}: {}", postId,  userId, ex.getMessage());
            return null;
        });
    }

    @Async
    @io.micronaut.http.annotation.Post //Because my class called Post also :D
    CompletableFuture<Post> savePost(@NonNull Post post){
        return service.savePost(post).exceptionally(ex -> {
            log.error("Error while save post: {}", ex.getMessage());
            return null;
        });
    }

    @Async
    @Delete("/{userId}/{postId}")
    CompletableFuture<MutableHttpResponse<Object>> deletePost(@NonNull @PathVariable String userId, @NonNull @PathVariable String postId){
        return service.deletePost(userId, postId).thenApply(_ -> HttpResponse.ok())
                .exceptionally(ex -> {
                    log.error("Error while delete post {} from user {}: {}", postId, userId, ex.getMessage());
                    return HttpResponse.serverError();
                });
    }
}
