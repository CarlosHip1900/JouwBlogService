package io.carloship.jouwblog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.carloship.jouwblog.repository.UserRedisRepository;
import io.carloship.jouwblog.response.impl.CachedUser;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class UserCache {

    private final UserRedisRepository repository;
    private final Cache<String, CachedUser> users;

    public UserCache(UserRedisRepository repository){
        this.repository = repository;
        this.users = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .removalListener((key, value, cause) -> {
                    if (!(key instanceof String id)) return;
                    if (!(value instanceof CachedUser user)) return;

                    if (cause == RemovalCause.EXPLICIT) return;
                    repository.saveUser(user.getUser());
                })
                .build();
    }

}
