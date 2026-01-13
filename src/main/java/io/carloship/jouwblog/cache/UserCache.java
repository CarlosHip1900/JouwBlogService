package io.carloship.jouwblog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.carloship.jouwblog.repository.UserRedisRepository;
import io.carloship.jouwblog.response.User;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Singleton
public class UserCache {

    private final Cache<String, User> usersByIdCache;
    private final Cache<String, User> usersByUsernameCache;
    private final UserRedisRepository repository;

    public UserCache(UserRedisRepository repository) {
        this.repository = repository;

        this.usersByUsernameCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .maximumSize(50_000)
                .build();

        this.usersByIdCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .maximumSize(50_000)
                .removalListener(this::onUserRemoval)
                .build();
    }

    private void onUserRemoval(String key, User value, RemovalCause cause) {
        if (key == null || value == null) return;

        if (cause == RemovalCause.EXPLICIT) return;

        usersByUsernameCache.invalidate(value.getUsername());

        saveToRedis(value);
    }

    private void saveToRedis(User user) {
        repository.saveUser(user)
                .thenAccept(result -> {
                    if (result) {
                        log.debug("User {} (ID: {}) saved to Redis", user.getUsername(), user.getId());
                    } else {
                        log.warn("User {} (ID: {}) could not be saved to Redis", user.getUsername(), user.getId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error saving user {} (ID: {}) to Redis: {}",
                            user.getUsername(), user.getId(), ex.getMessage(), ex);
                    return null;
                });
    }

    public void addUser(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(user.getId(), "user ID cannot be null");
        Objects.requireNonNull(user.getUsername(), "username cannot be null");

        usersByIdCache.put(user.getId(), user);
        usersByUsernameCache.put(user.getUsername(), user);
    }

    public User getUserById(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return usersByIdCache.getIfPresent(userId);
    }

    public User getUserByUsername(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        return usersByUsernameCache.getIfPresent(username);
    }

    public void invalidate(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        var user = getUserById(userId);
        if (user != null) {
            usersByUsernameCache.invalidate(user.getUsername());
            usersByIdCache.invalidate(userId);
        }
    }

    public void invalidateByUsername(String username) {
        Objects.requireNonNull(username, "username cannot be null");

        var user = getUserByUsername(username);
        if (user != null) {
            usersByIdCache.invalidate(user.getId());
            usersByUsernameCache.invalidate(username);
        }
    }

    public void updateUser(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(user.getId(), "user ID cannot be null");
        Objects.requireNonNull(user.getUsername(), "username cannot be null");

        // Check if username changed
        var existingUser = getUserById(user.getId());
        if (existingUser != null && !existingUser.getUsername().equals(user.getUsername())) {
            // Username changed, remove old username from cache
            usersByUsernameCache.invalidate(existingUser.getUsername());
        }

        usersByIdCache.put(user.getId(), user);
        usersByUsernameCache.put(user.getUsername(), user);
    }

    public void clear() {
        usersByIdCache.invalidateAll();
        usersByUsernameCache.invalidateAll();
    }
}