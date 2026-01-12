package io.carloship.jouwblog.service;

import io.carloship.jouwblog.cache.UserCache;
import io.carloship.jouwblog.repository.UserRedisRepository;
import io.carloship.jouwblog.repository.UserRepository;
import io.carloship.jouwblog.response.Comment;
import io.carloship.jouwblog.response.User;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class UserService {

    @Inject
    protected UserRepository repository;

    @Inject
    protected UserRedisRepository redisRepository;

    @Inject
    protected UserCache cache;

    @Async
    public CompletableFuture<User> findUser(String userId){
        User cached = cache.getUserById(userId);
        if (cached != null){
            return CompletableFuture.completedFuture(cached);
        }

        return redisRepository.findUser(userId).thenCompose(redisUser -> {
            if (redisUser != null){
                cache.addCachedUser(redisUser);
                return CompletableFuture.completedFuture(redisUser);
            }

            return repository.findById(userId).thenApply(mongo -> {
                if (mongo != null){
                    cache.addCachedUser(mongo);
                    redisRepository.saveUser(mongo);
                    return mongo;
                }

                return null;
            }).exceptionally(ex -> {
                log.error("Error while find user {} in mongo: {}", userId, ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            log.error("Error while find user {} in redis: {}", userId, ex.getMessage());
            return null;
        });
    }

}
