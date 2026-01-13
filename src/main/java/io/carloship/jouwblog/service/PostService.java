package io.carloship.jouwblog.service;

import io.carloship.jouwblog.cache.PostCache;
import io.carloship.jouwblog.repository.PostRedisRepository;
import io.carloship.jouwblog.repository.PostRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PostService {

    @Inject
    protected PostRepository repository;

    @Inject
    protected PostRedisRepository redisRepository;

    @Inject
    protected PostCache postCache;
}
