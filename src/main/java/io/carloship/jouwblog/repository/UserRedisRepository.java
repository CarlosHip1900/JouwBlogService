package io.carloship.jouwblog.repository;

import io.carloship.jouwblog.Application;
import io.carloship.jouwblog.response.User;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class UserRedisRepository {

    private static final String PREFIX = "user:";
    private static final String PREFIX_SEARCH_USERNAME = "user_search:";
    private static final Duration DEFAULT_EXPIRATION_SECONDS = Application.DEFAULT_REDIS_TIME;

    private final RedisAsyncCommands<String, String> asyncCommands;

    @Inject
    public UserRedisRepository(RedisAsyncCommands<String, String> asyncCommands) {
        this.asyncCommands = asyncCommands;
    }

    @Async
    public CompletableFuture<User> findUser(@NonNull String id) {
        if (id.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        String key = buildUserKey(id);
        return asyncCommands.hgetall(key)
                .thenApply(mappedResult -> {
                    if (mappedResult == null || mappedResult.isEmpty()) {
                        log.debug("User not found: id={}", id);
                        return null;
                    }

                    log.debug("Found user: id={}", id);
                    User user = new User();
                    return user.fromMap(mappedResult);
                })
                .exceptionally(ex -> {
                    log.error("Error finding user: id={}, error={}", id, ex.getMessage(), ex);
                    return null;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<User> searchUser(String username) {
        if (username == null || username.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        String searchKey = buildUsernameSearchKey(username);
        return asyncCommands.get(searchKey)
                .thenCompose(userId -> {
                    if (userId == null || userId.isEmpty()) {
                        log.debug("User not found by username: {}", username);
                        return CompletableFuture.completedFuture(null);
                    }
                    return findUser(userId);
                })
                .exceptionally(ex -> {
                    log.error("Error searching user by username: username={}, error={}",
                            username, ex.getMessage(), ex);
                    return null;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> saveUser(@NonNull User user) {
        if (user.getId() == null || user.getId().isBlank()
                || user.getUsername() == null || user.getUsername().isBlank()) {
            log.warn("Invalid user data for save operation");
            return CompletableFuture.completedFuture(false);
        }

        var map = user.toMap();
        String userKey = buildUserKey(user.getId());
        String usernameKey = buildUsernameSearchKey(user.getUsername());

        // Save user hash
        return asyncCommands.hset(userKey, map)
                .thenCompose(_ -> {
                    // Save username -> userId mapping
                    return asyncCommands.set(usernameKey, user.getId());
                })
                .thenCompose(_ -> {
                    // Set expiration for user hash
                    return asyncCommands.expire(userKey, DEFAULT_EXPIRATION_SECONDS)
                            .thenCompose(expireResult -> {
                                // Set expiration for username search key
                                return asyncCommands.expire(usernameKey, DEFAULT_EXPIRATION_SECONDS);
                            });
                })
                .thenApply(expireResult -> {
                    log.debug("User saved successfully: id={}, username={}",
                            user.getId(), user.getUsername());
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Error saving user: id={}, username={}, error={}",
                            user.getId(), user.getUsername(), ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> deleteUser(@NonNull String id) {
        if (id.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String userKey = buildUserKey(id);

        // First, get the username to delete the search index
        return asyncCommands.hget(userKey, "username")
                .thenCompose(username -> {
                    CompletableFuture<Long> deleteUserFuture = asyncCommands.del(userKey).toCompletableFuture();

                    if (username != null && !username.isBlank()) {
                        String usernameKey = buildUsernameSearchKey(username);
                        CompletableFuture<Long> deleteUsernameFuture = asyncCommands.del(usernameKey).toCompletableFuture();

                        return deleteUserFuture.thenCombine(deleteUsernameFuture, (r1, r2) -> r1 + r2);
                    }

                    return deleteUserFuture;
                })
                .thenApply(deletedCount -> {
                    boolean success = deletedCount > 0;
                    if (success) {
                        log.debug("User deleted: id={}", id);
                    } else {
                        log.warn("User not found for deletion: id={}", id);
                    }
                    return success;
                })
                .exceptionally(ex -> {
                    log.error("Error deleting user: id={}, error={}", id, ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    @Async
    public CompletableFuture<Boolean> updateUsername(@NonNull String userId,@NonNull String newUsername) {
        if (userId.isBlank() || newUsername.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String userKey = buildUserKey(userId);

        // Get old username to delete old search key
        return asyncCommands.hget(userKey, "username")
                .thenCompose(oldUsername -> {
                    // Update username in user hash
                    return asyncCommands.hset(userKey, "username", newUsername)
                            .thenCompose(hsetResult -> {
                                // Delete old username search key if it exists
                                CompletableFuture<?> deleteOldFuture = CompletableFuture.completedFuture(null);
                                if (oldUsername != null && !oldUsername.isBlank() && !oldUsername.equals(newUsername)) {
                                    String oldUsernameKey = buildUsernameSearchKey(oldUsername);
                                    deleteOldFuture = asyncCommands.del(oldUsernameKey).thenApply(v -> null).toCompletableFuture();
                                }

                                // Create new username search key
                                String newUsernameKey = buildUsernameSearchKey(newUsername);
                                return deleteOldFuture.thenCompose(v ->
                                        asyncCommands.set(newUsernameKey, userId)
                                                .thenCompose(setResult ->
                                                        asyncCommands.expire(newUsernameKey, DEFAULT_EXPIRATION_SECONDS)
                                                )
                                );
                            });
                })
                .thenApply(result -> {
                    log.debug("Username updated: userId={}, newUsername={}", userId, newUsername);
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Error updating username: userId={}, newUsername={}, error={}",
                            userId, newUsername, ex.getMessage(), ex);
                    return false;
                })
                .toCompletableFuture();
    }

    private String buildUserKey(String userId) {
        return PREFIX + userId;
    }

    private String buildUsernameSearchKey(String username) {
        return PREFIX_SEARCH_USERNAME + username.toLowerCase();
    }
}