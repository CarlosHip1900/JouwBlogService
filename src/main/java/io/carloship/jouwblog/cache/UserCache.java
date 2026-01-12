package io.carloship.jouwblog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.carloship.jouwblog.repository.UserRedisRepository;
import io.carloship.jouwblog.response.User;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Singleton
public class UserCache {

    private final UserRedisRepository repository;
    private final Cache<String, User> users;
    private final Cache<String, User> byUsername;

    public UserCache(UserRedisRepository repository){
        this.repository = repository;
        this.byUsername = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(15)).build();
        this.users = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .removalListener((key, value, cause) -> {
                    if (!(key instanceof String id)) return;
                    if (!(value instanceof User user)) return;

                    if (cause == RemovalCause.EXPLICIT) return;
                    byUsername.invalidate(user.getUsername());
                    repository.saveUser(user).thenAccept(result -> {
                        if (result){
                            log.info("User {} saved in redis", user.getName());
                            return;
                        }

                        log.error("User {} isn't saved in redis", user.getName());
                    }).exceptionally(ex -> {
                        log.error("Error while saving user {} in redis: {}", user.getName(), ex.getMessage());
                        return null;
                    });
                })
                .build();
    }

    public void addCachedUser(Supplier<User> supplier){
        var obj = supplier.get();
        addCachedUser(obj);
    }

    public void addCachedUser(User user){
        users.put(user.getName(), user);
    }

    public User getUserById(String userId){
        return users.getIfPresent(userId);
    }

    public User getUserByUsername(String userName){
        return byUsername.getIfPresent(userName);
    }
}
