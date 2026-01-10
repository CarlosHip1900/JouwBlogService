package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.response.Post;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.CrudRepository;

@MongoRepository
public interface PostRepository extends CrudRepository<Post, String> {
    
    Page<Post> findByUserId(String userId, Pageable pageable);
    
    long countByUserId(String userId);
}