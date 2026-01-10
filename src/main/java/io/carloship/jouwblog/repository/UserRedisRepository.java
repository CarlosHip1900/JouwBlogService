package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.Application;
import io.carloship.jouwblog.response.User;
import io.carloship.jouwblog.runtime.BlogThreads;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class UserRedisRepository {

    private static final String PREFIX = "user:";
    private static final String PREFIX_SEARCH_ID = "user_search:";

    @Inject
    RedisAsyncCommands<String, String> asyncCommands;

    @Async
    public CompletableFuture<User> findUser(String id) {
        String key = PREFIX + id;
        return asyncCommands.hgetall(key).thenApply(mappedResult -> {
                    if (mappedResult == null) {
                        log.warn("Not found mapped result of user {}", id);
                        return null;
                    }

                    if (mappedResult.isEmpty()) {
                        log.warn("Mapped result was found, however is empty...");
                        return null;
                    }

                    log.info("Found user {}, building data....", id);
                    User user = new User();
                    return user.fromMap(mappedResult);
                }).exceptionally(ex -> {
                    log.error("Error while finding user {}: {}", id, ex.getMessage());
                    return null;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<User> searchUser(String name) {
        String key = PREFIX_SEARCH_ID + name;
        return asyncCommands.get(key).thenCompose(fk -> {
            if (fk == null || fk.isEmpty()) {
                log.warn("Not found user with name {}", name);
                return CompletableFuture.completedFuture(null);
            }

            return findUser(fk);
        }).exceptionally(ex -> {
            log.error("Error while search user {}: {}", name, ex.getMessage());
            return null;
        }).toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> saveUser(User user) {
        var map = user.toMap();
        String key = PREFIX + user.getId();
        String name_key = PREFIX_SEARCH_ID + user.getUsername();

        return asyncCommands.hset(key, map).thenCompose(rows -> {
            //if (rows != 0) {}

            return asyncCommands.set(name_key, user.getId()).thenApply(resultOperation -> {
                //Expire
                asyncCommands.expire(key, Application.DEFAULT_REDIS_TIME);
                asyncCommands.expire(name_key, Application.DEFAULT_REDIS_TIME);

                return resultOperation.equalsIgnoreCase("OK");
            }).exceptionally(ex -> {
                log.error("Error while saving index of user id {}: {}", user.getId(), ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            log.error("Error while saving whole user  {}: {}", user.getUsername(), ex.getMessage());
            return null;
        }).toCompletableFuture();
    }


}
