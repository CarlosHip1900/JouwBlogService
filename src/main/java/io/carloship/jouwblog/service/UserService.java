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
import org.jspecify.annotations.NonNull;

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
    public CompletableFuture<User> findUser(@NonNull String userId){
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

    @Async
    public CompletableFuture<User> saveUser(@NonNull User user){
        if (user.getId() == null){
            return repository.save(user).thenCompose(savedUser -> {
                if (savedUser.getId() == null || savedUser.getId().isBlank()){
                    log.warn("User id isn't created in mongo... back end problem...");
                    return CompletableFuture.completedFuture(null);
                }

                return redisRepository.saveUser(savedUser).thenApply(operation -> {
                    if (!operation){
                        log.warn("Cannot possible add user in redis...");
                        return null;
                    }

                    cache.addCachedUser(user);
                    return savedUser;
                });
            });
        }

        return repository.update(user).thenCompose(updatedUser -> {
            log.debug("Updating user {} in redis...", updatedUser.getId());
            return redisRepository.saveUser(updatedUser).thenApply(operation -> {
                if (!operation){
                    log.warn("Cannot possible update user in redis...");
                    return null;
                }

                cache.addCachedUser(updatedUser);
                return updatedUser;
            }).exceptionally(ex -> {
                log.error("Error while update user in redis {}: {}", user.getId(), ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            log.error("Error while update user in mongo {}: {}", user.getId(), ex.getMessage());
            return null;
        });
    }


    @Async
    public CompletableFuture<Void> deleteUser(@NonNull String id){
        return repository.deleteById(id).thenCompose(_ -> redisRepository.deleteUser(id).thenAccept(_2 -> cache.invalid(id)));
    }
}
