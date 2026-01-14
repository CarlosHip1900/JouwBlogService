package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.response.Post;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.async.AsyncCrudRepository;

import java.util.concurrent.CompletableFuture;

@MongoRepository
public interface PostRepository extends AsyncCrudRepository<Post, String> {
    
    CompletableFuture<Page<Post>> findPostsByUserId(String userId, Pageable pageable);

    CompletableFuture<Post> findByUserId(String userId, String postId);

    long countByUserId(String userId);
}