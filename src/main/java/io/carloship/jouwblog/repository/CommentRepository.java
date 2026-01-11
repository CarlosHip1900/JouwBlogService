package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.response.Comment;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.CrudRepository;

@MongoRepository
public interface CommentRepository extends CrudRepository<Comment, String> {
    
    Page<Comment> findByPostId(String postId, Pageable pageable);
    
}