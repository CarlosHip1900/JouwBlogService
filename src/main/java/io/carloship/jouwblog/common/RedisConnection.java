package io.carloship.jouwblog.common;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Factory
public class RedisConnection {

    @Value("${redis.uri}")
    private String url;

    @Singleton
    RedisClient client(){
        return RedisClient.create(url);
    }

    @Singleton
    StatefulRedisConnection<String, String> connection(){
        return client().connect();
    }

    @Singleton
    StatefulRedisPubSubConnection<String, String> pubSubConnection(){
        return client().connectPubSub();
    }

    @Singleton
    RedisCommands<String, String> syncCommands(){
        return connection().sync();
    }

    @Singleton
    RedisAsyncCommands<String, String> asyncCommands(){
        return connection().async();
    }

    @PreDestroy
    public void close(){
        client().close();
    }
}
