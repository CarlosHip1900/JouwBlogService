package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.response.User;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.async.AsyncCrudRepository;

@MongoRepository
public interface UserRepository extends AsyncCrudRepository<User, String> {

}
